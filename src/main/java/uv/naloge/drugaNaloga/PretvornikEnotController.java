package uv.naloge.drugaNaloga;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PretvornikEnotController {

    private static final String LENGTH_CATEGORY = "Dolžina";
    private static final String TEMPERATURE_CATEGORY = "Temperatura";
    private static final String MASS_CATEGORY = "Masa";

    private static final String METER_UNIT = "m";
    private static final String CENTIMETER_UNIT = "cm";
    private static final String INCH_UNIT = "in";
    private static final String FOOT_UNIT = "ft";

    private static final String CELSIUS_UNIT = "°C";
    private static final String FAHRENHEIT_UNIT = "°F";

    private static final String KILOGRAM_UNIT = "kg";
    private static final String GRAM_UNIT = "g";
    private static final String OUNCE_UNIT = "oz";

    private static final String AUTHOR_INFORMATION = "Avtor: Mai Rupnik, 2. letnik, BVS-RI @ FRI UNI-LJ";
    private static final String PROJECT_URL = "https://github.com/mairup/uv-naloga-2";
    private static final String HISTORY_FILE_DELIMITER = "\t";
    private static final String HISTORY_FILE_HEADER = "# firstValue\tfirstUnit\tsecondValue\tsecondUnit";
    private static final String TEXT_FILE_EXTENSION_PATTERN = "*.txt";
    private static final String LOG_FILE_EXTENSION_PATTERN = "*.log";
    private static final String LOG_FILE_EXTENSION = ".log";
    private static final String TEXT_FILE_EXTENSION = ".txt";
    private static final String STATUS_ERROR_STYLE_CLASS = "status-label-error";
    private static final String PREVIEW_TARGET_VALUE_STYLE_CLASS = "preview-conversion-value";
    private static final int MAX_EVENT_LOG_ENTRIES = 1000;

    @FXML
    private TextField firstValueTextField;

    @FXML
    private TextField secondValueTextField;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private ComboBox<String> firstUnitComboBox;

    @FXML
    private ComboBox<String> secondUnitComboBox;

    @FXML
    private VBox historyTilesContainer;

    @FXML
    private ScrollPane historyScrollPane;

    @FXML
    private TextArea eventLogTextArea;

    @FXML
    private Label statusLabel;

    @FXML
    private TitledPane pane1;

    @FXML
    private TitledPane pane2;

    @FXML
    private TitledPane pane3;

    @FXML
    private Accordion accordionContainer;

    @FXML
    private Button openToolbarButton;

    @FXML
    private Button saveToolbarButton;

    @FXML
    private Button clearToolbarButton;

    @FXML
    private Button exitToolbarButton;

    @FXML
    private Button directionToggleButton;

    private final Map<String, List<String>> unitsByCategory = new LinkedHashMap<>();
    private final DecimalFormat numberFormatter = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance());
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final ObservableList<HistoryEntry> historyEntries = FXCollections.observableArrayList();
    private final ObservableList<String> eventLogEntries = FXCollections.observableArrayList();
    private boolean isFirstToSecondConversion = true;
    private boolean isStatusResetQueued;
    private boolean isSynchronizingUnitSelection;
    private boolean isNumericKeyboardFocusHandlerInstalled;

    public void initialize() {
        initializeCategoriesAndUnits();
        initializeDynamicConversionBehavior();
        installNumericKeyboardFocusBehavior();
        initializeToolbarIcons();
        expandOnlyPane(pane1);
        renderHistoryTiles();
        updateDirectionToggleState();
        updateInputFieldInteractivityForDirection();
        applyPreviewStyleToCurrentTarget(false);
        eventLogTextArea.setWrapText(false);
        statusLabel.setText("Pripravljeno.");
        appendEventLog("Aplikacija je pripravljena za delo.");
    }

    private void expandOnlyPane(TitledPane paneToExpand) {
        if (paneToExpand == null || accordionContainer == null) {
            return;
        }
        accordionContainer.setExpandedPane(paneToExpand);
    }

    private void initializeCategoriesAndUnits() {
        unitsByCategory.put(LENGTH_CATEGORY, List.of(METER_UNIT, CENTIMETER_UNIT, INCH_UNIT, FOOT_UNIT));
        unitsByCategory.put(TEMPERATURE_CATEGORY, List.of(CELSIUS_UNIT, FAHRENHEIT_UNIT));
        unitsByCategory.put(MASS_CATEGORY, List.of(KILOGRAM_UNIT, GRAM_UNIT, OUNCE_UNIT));

        categoryComboBox.getItems().setAll(unitsByCategory.keySet());
        categoryComboBox.getSelectionModel().selectFirst();
        updateUnitsForSelectedCategory();
    }

    private void initializeToolbarIcons() {
        openToolbarButton.setGraphic(new FontIcon("fas-folder-open"));
        saveToolbarButton.setGraphic(new FontIcon("fas-save"));
        clearToolbarButton.setGraphic(new FontIcon("fas-eraser"));
        exitToolbarButton.setGraphic(new FontIcon("fas-sign-out-alt"));
    }

    private void initializeDynamicConversionBehavior() {
        firstValueTextField.textProperty()
                .addListener((observableValue, oldText, newText) -> onConversionValueChanged(firstValueTextField));
        secondValueTextField.textProperty()
                .addListener((observableValue, oldText, newText) -> onConversionValueChanged(secondValueTextField));

        firstUnitComboBox.valueProperty()
                .addListener((observableValue, oldUnit, newUnit) -> onUnitSelectionChanged());
        secondUnitComboBox.valueProperty()
                .addListener((observableValue, oldUnit, newUnit) -> onUnitSelectionChanged());
    }

    private void installNumericKeyboardFocusBehavior() {
        firstValueTextField.sceneProperty()
                .addListener((observableValue, oldScene, newScene) -> registerNumericKeyboardFocusHandler(newScene));
        registerNumericKeyboardFocusHandler(firstValueTextField.getScene());
    }

    private void registerNumericKeyboardFocusHandler(Scene scene) {
        if (scene == null || isNumericKeyboardFocusHandlerInstalled) {
            return;
        }

        scene.addEventFilter(KeyEvent.KEY_TYPED, this::onNumericKeyTyped);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onCommandKeyPressed);
        isNumericKeyboardFocusHandlerInstalled = true;
    }

    private void onNumericKeyTyped(KeyEvent keyEvent) {
        if (!pane1.isExpanded()) {
            return;
        }

        String typedCharacterText = keyEvent.getCharacter();
        if (typedCharacterText == null || typedCharacterText.isEmpty() || typedCharacterText.length() != 1) {
            return;
        }

        char typedCharacter = typedCharacterText.charAt(0);
        if (!isNumericTypingCharacter(typedCharacter)) {
            return;
        }

        TextField sourceValueField = getCurrentSourceValueField();

        if (sourceValueField.isFocused()) {
            return;
        }

        String currentText = sourceValueField.getText();
        boolean alreadyContainsDecimalSeparator = currentText != null
                && (currentText.contains(".") || currentText.contains(","));
        if ((typedCharacter == '.' || typedCharacter == ',') && alreadyContainsDecimalSeparator) {
            keyEvent.consume();
            return;
        }

        if (typedCharacter == '-' && currentText != null && !currentText.isBlank()) {
            keyEvent.consume();
            return;
        }

        String normalizedInput = typedCharacter == ',' ? "." : String.valueOf(typedCharacter);
        sourceValueField.requestFocus();
        sourceValueField.positionCaret(sourceValueField.getLength());
        sourceValueField.appendText(normalizedInput);
        keyEvent.consume();
    }

    private void onCommandKeyPressed(KeyEvent keyEvent) {
        if (!pane1.isExpanded()) {
            return;
        }

        if (keyEvent.getCode() == KeyCode.ENTER && !keyEvent.isAltDown() && !keyEvent.isControlDown()
                && !keyEvent.isMetaDown()) {
            onConvertClick();
            keyEvent.consume();
            return;
        }

        if (keyEvent.getCode() != KeyCode.BACK_SPACE) {
            return;
        }

        TextField sourceValueField = getCurrentSourceValueField();
        if (sourceValueField == null || !sourceValueField.isEditable()) {
            return;
        }

        if (sourceValueField.isFocused()) {
            return;
        }

        sourceValueField.requestFocus();
        sourceValueField.positionCaret(sourceValueField.getLength());

        if (keyEvent.isControlDown()) {
            deletePreviousWordFromEnd(sourceValueField);
        } else {
            deleteLastCharacter(sourceValueField);
        }

        keyEvent.consume();
    }

    private void deleteLastCharacter(TextField sourceValueField) {
        String currentText = sourceValueField.getText();
        if (currentText == null || currentText.isEmpty()) {
            return;
        }

        sourceValueField.setText(currentText.substring(0, currentText.length() - 1));
        sourceValueField.positionCaret(sourceValueField.getLength());
    }

    private void deletePreviousWordFromEnd(TextField sourceValueField) {
        String currentText = sourceValueField.getText();
        if (currentText == null || currentText.isEmpty()) {
            return;
        }

        int endIndex = currentText.length();
        while (endIndex > 0 && Character.isWhitespace(currentText.charAt(endIndex - 1))) {
            endIndex--;
        }

        int startIndex = endIndex;
        while (startIndex > 0 && !Character.isWhitespace(currentText.charAt(startIndex - 1))) {
            startIndex--;
        }

        sourceValueField.setText(currentText.substring(0, startIndex));
        sourceValueField.positionCaret(sourceValueField.getLength());
    }

    private boolean isNumericTypingCharacter(char inputCharacter) {
        return Character.isDigit(inputCharacter) || inputCharacter == '.' || inputCharacter == ','
                || inputCharacter == '-';
    }

    private void onConversionValueChanged(TextField changedField) {
        if (changedField != getCurrentSourceValueField()) {
            return;
        }
        updateLiveConversionPreview();
    }

    private void onUnitSelectionChanged() {
        if (isSynchronizingUnitSelection) {
            return;
        }
        updateUnitsForSelectedCategory();
    }

    @FXML
    private void onCategoryChanged() {
        updateUnitsForSelectedCategory();
        String selectedCategory = categoryComboBox.getValue();
        updateStatusAndLog("Izbrana kategorija: " + selectedCategory + ".");
    }

    private void updateUnitsForSelectedCategory() {
        String selectedCategory = categoryComboBox.getValue();
        List<String> units = unitsByCategory.getOrDefault(selectedCategory, List.of());

        ComboBox<String> sourceUnitComboBox = getCurrentSourceUnitField();
        ComboBox<String> targetUnitComboBox = getCurrentTargetUnitField();

        String previouslySelectedSourceUnit = sourceUnitComboBox.getValue();
        String previouslySelectedTargetUnit = targetUnitComboBox.getValue();

        isSynchronizingUnitSelection = true;
        try {
            sourceUnitComboBox.getItems().setAll(units);
            if (!units.isEmpty()) {
                if (previouslySelectedSourceUnit != null && units.contains(previouslySelectedSourceUnit)) {
                    sourceUnitComboBox.getSelectionModel().select(previouslySelectedSourceUnit);
                } else {
                    sourceUnitComboBox.getSelectionModel().selectFirst();
                }
            } else {
                sourceUnitComboBox.getSelectionModel().clearSelection();
            }

            String currentlySelectedSourceUnit = sourceUnitComboBox.getValue();
            List<String> targetUnits = units.stream()
                    .filter(unit -> !Objects.equals(unit, currentlySelectedSourceUnit))
                    .toList();

            targetUnitComboBox.getItems().setAll(targetUnits);
            if (!targetUnits.isEmpty()) {
                if (previouslySelectedTargetUnit != null && targetUnits.contains(previouslySelectedTargetUnit)) {
                    targetUnitComboBox.getSelectionModel().select(previouslySelectedTargetUnit);
                } else {
                    targetUnitComboBox.getSelectionModel().selectFirst();
                }
            } else {
                targetUnitComboBox.getSelectionModel().clearSelection();
            }
        } finally {
            isSynchronizingUnitSelection = false;
        }

        updateLiveConversionPreview();
    }

    @FXML
    private void onNumericKeypadButtonClick(ActionEvent actionEvent) {
        Object userDataValue = ((Button) actionEvent.getSource()).getUserData();
        if (userDataValue == null) {
            return;
        }

        String inputAppendix = userDataValue.toString();
        TextField sourceValueField = getCurrentSourceValueField();
        String currentInputValue = sourceValueField.getText();
        if (".".equals(inputAppendix) && currentInputValue.contains(".")) {
            return;
        }
        sourceValueField.setText(currentInputValue + inputAppendix);
    }

    @FXML
    private void onClearInputClick() {
        getCurrentSourceValueField().clear();
        updateStatusAndLog("Vnos v pretvorniku je bil pobrisan.");
    }

    @FXML
    private void onToggleDirectionClick() {
        isFirstToSecondConversion = !isFirstToSecondConversion;
        updateDirectionToggleState();
        updateUnitsForSelectedCategory();
        updateInputFieldInteractivityForDirection();
        updateStatusAndLog("Smer pretvorbe je nastavljena na " + getDirectionDescription() + ".");
    }

    @FXML
    private void onConvertClick() {
        TextField sourceValueField = getCurrentSourceValueField();
        TextField targetValueField = getCurrentTargetValueField();
        ComboBox<String> sourceUnitField = getCurrentSourceUnitField();
        ComboBox<String> targetUnitField = getCurrentTargetUnitField();

        String inputText = sourceValueField.getText().trim().replace(',', '.');
        if (inputText.isEmpty()) {
            updateStatusAndLogError("Napaka: vnos je prazen.");
            return;
        }

        String selectedCategory = categoryComboBox.getValue();
        String selectedSourceUnit = sourceUnitField.getValue();
        String selectedTargetUnit = targetUnitField.getValue();

        if (selectedCategory == null || selectedSourceUnit == null || selectedTargetUnit == null) {
            updateStatusAndLogError("Napaka: kategorija ali enota ni izbrana.");
            return;
        }

        Double inputNumericValue = tryParseDouble(inputText);
        if (inputNumericValue == null) {
            updateStatusAndLogError("Napaka pri vnosu: " + inputText + ".");
            return;
        }

        double convertedValue;
        try {
            convertedValue = convertValue(selectedCategory, selectedSourceUnit, selectedTargetUnit,
                    inputNumericValue);
        } catch (IllegalStateException exception) {
            updateStatusAndLogError("Napaka pretvorbe: " + exception.getMessage());
            return;
        }
        String sourceValueText = formatNumber(inputNumericValue);
        String targetValueText = formatNumber(convertedValue);

        HistoryEntry historyEntry = new HistoryEntry(
                sourceValueText,
                selectedSourceUnit,
                targetValueText,
                selectedTargetUnit);
        String conversionEntry = historyEntry.getFirstDisplay() + " -> " + historyEntry.getSecondDisplay();

        targetValueField.setText(targetValueText);
        applyPreviewStyleToCurrentTarget(false);
        if (isSameAsLastHistoryEntry(historyEntry)) {
            updateStatusAndLog("Pretvorba je enaka zadnji, zato ni bila dodana v zgodovino: " + conversionEntry + ".");
            return;
        }
        appendConversionToHistory(historyEntry);
        updateStatusAndLog("Izvedena pretvorba: " + conversionEntry + ".");
    }

    private boolean isSameAsLastHistoryEntry(HistoryEntry currentEntry) {
        if (historyEntries.isEmpty()) {
            return false;
        }
        HistoryEntry lastEntry = historyEntries.get(historyEntries.size() - 1);
        return currentEntry.equals(lastEntry);
    }

    private Double tryParseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void restoreHistoryEntry(HistoryEntry entry) {
        expandOnlyPane(pane1);
        resetDirectionToFirstDownwardIfNeeded();

        String targetCategory = getCategoryForUnit(entry.getFirstUnit());
        if (targetCategory != null && !targetCategory.equals(categoryComboBox.getValue())) {
            categoryComboBox.getSelectionModel().select(targetCategory);
            updateUnitsForSelectedCategory();
        }

        firstValueTextField.setText(entry.getFirstValueText());
        secondValueTextField.setText(entry.getSecondValueText());
        selectUnitIfAvailable(firstUnitComboBox, entry.getFirstUnit());
        selectUnitIfAvailable(secondUnitComboBox, entry.getSecondUnit());
        applyPreviewStyleToCurrentTarget(false);

        updateStatusAndLog("Obnovljena pretvorba: " + entry.getFirstDisplay() + " -> " + entry.getSecondDisplay()
                + ".");
    }

    private void resetDirectionToFirstDownwardIfNeeded() {
        if (!isFirstToSecondConversion) {
            isFirstToSecondConversion = true;
            updateDirectionToggleState();
            updateUnitsForSelectedCategory();
            appendEventLog("Smer pretvorbe je bila ob obnovitvi nastavljena navzdol.");
        }
        updateInputFieldInteractivityForDirection();
    }

    private void applyEventLogRetentionLimit() {
        int exceededEntries = eventLogEntries.size() - MAX_EVENT_LOG_ENTRIES;
        if (exceededEntries <= 0) {
            return;
        }
        eventLogEntries.remove(0, exceededEntries);
    }

    private void scrollEventLogToBottom() {
        if (eventLogTextArea == null) {
            return;
        }

        Platform.runLater(() -> {
            eventLogTextArea.setScrollTop(Double.MAX_VALUE);
            eventLogTextArea.setScrollLeft(0);
            eventLogTextArea.deselect();
        });
    }

    private void refreshVisibleEventLog() {
        if (eventLogTextArea == null) {
            return;
        }

        if (eventLogEntries.isEmpty()) {
            eventLogTextArea.clear();
            return;
        }

        eventLogTextArea.setText(String.join(System.lineSeparator(), eventLogEntries));
        scrollEventLogToBottom();
    }

    private void replaceEventLogContent(List<String> loadedLogLines) {
        eventLogEntries.setAll(loadedLogLines);
        applyEventLogRetentionLimit();
        refreshVisibleEventLog();
    }

    private void appendLogEntryLine(String entryLine) {
        eventLogEntries.add(entryLine);
        applyEventLogRetentionLimit();
        refreshVisibleEventLog();
    }

    private void selectUnitIfAvailable(ComboBox<String> comboBox, String unit) {
        if (comboBox.getItems().contains(unit)) {
            comboBox.getSelectionModel().select(unit);
            return;
        }

        appendEventLog("Nepodprta enota pri obnovi zapisa: " + unit);
    }

    private String getCategoryForUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        for (Map.Entry<String, List<String>> mapEntry : unitsByCategory.entrySet()) {
            if (mapEntry.getValue().contains(unit)) {
                return mapEntry.getKey();
            }
        }
        return null;
    }

    private TextField getCurrentSourceValueField() {
        return isFirstToSecondConversion ? firstValueTextField : secondValueTextField;
    }

    private TextField getCurrentTargetValueField() {
        return isFirstToSecondConversion ? secondValueTextField : firstValueTextField;
    }

    private ComboBox<String> getCurrentSourceUnitField() {
        return isFirstToSecondConversion ? firstUnitComboBox : secondUnitComboBox;
    }

    private ComboBox<String> getCurrentTargetUnitField() {
        return isFirstToSecondConversion ? secondUnitComboBox : firstUnitComboBox;
    }

    private String getDirectionDescription() {
        return isFirstToSecondConversion ? "prve navzdol proti drugi" : "druge navzgor proti prvi";
    }

    private void updateInputFieldInteractivityForDirection() {
        TextField sourceField = getCurrentSourceValueField();
        TextField targetField = getCurrentTargetValueField();

        sourceField.setEditable(true);
        sourceField.setFocusTraversable(true);
        sourceField.setMouseTransparent(false);

        targetField.setEditable(false);
        targetField.setFocusTraversable(false);
        targetField.setMouseTransparent(true);
        targetField.deselect();

        if (targetField.isFocused()) {
            Platform.runLater(sourceField::requestFocus);
        }
    }

    private void applyPreviewStyleToCurrentTarget(boolean showPreviewStyle) {
        firstValueTextField.getStyleClass().remove(PREVIEW_TARGET_VALUE_STYLE_CLASS);
        secondValueTextField.getStyleClass().remove(PREVIEW_TARGET_VALUE_STYLE_CLASS);

        if (!showPreviewStyle) {
            return;
        }

        TextField targetValueField = getCurrentTargetValueField();
        if (!targetValueField.getStyleClass().contains(PREVIEW_TARGET_VALUE_STYLE_CLASS)) {
            targetValueField.getStyleClass().add(PREVIEW_TARGET_VALUE_STYLE_CLASS);
        }
    }

    private void updateLiveConversionPreview() {
        TextField sourceValueField = getCurrentSourceValueField();
        TextField targetValueField = getCurrentTargetValueField();

        String inputText = sourceValueField.getText();
        if (inputText == null || inputText.isBlank()) {
            targetValueField.clear();
            applyPreviewStyleToCurrentTarget(false);
            return;
        }

        String selectedCategory = categoryComboBox.getValue();
        String selectedSourceUnit = getCurrentSourceUnitField().getValue();
        String selectedTargetUnit = getCurrentTargetUnitField().getValue();

        Double sourceNumericValue = tryParseDouble(inputText.trim().replace(',', '.'));
        if (sourceNumericValue == null) {
            targetValueField.clear();
            applyPreviewStyleToCurrentTarget(false);
            return;
        }

        try {
            double convertedValue = convertValue(selectedCategory, selectedSourceUnit, selectedTargetUnit,
                    sourceNumericValue);
            targetValueField.setText(formatNumber(convertedValue));
            applyPreviewStyleToCurrentTarget(true);
        } catch (IllegalStateException exception) {
            targetValueField.clear();
            applyPreviewStyleToCurrentTarget(false);
        }
    }

    private void updateDirectionToggleState() {
        if (directionToggleButton == null) {
            return;
        }
        directionToggleButton.setText(isFirstToSecondConversion ? "↓" : "↑");
    }

    private double convertValue(String category, String sourceUnit, String targetUnit, double value) {
        if (sourceUnit.equals(targetUnit)) {
            return value;
        }

        return switch (category) {
            case LENGTH_CATEGORY -> convertMetersToTargetUnit(targetUnit, convertLengthToMeters(sourceUnit, value));
            case TEMPERATURE_CATEGORY -> convertTemperature(sourceUnit, targetUnit, value);
            case MASS_CATEGORY -> convertKilogramsToTargetUnit(targetUnit, convertMassToKilograms(sourceUnit, value));
            default -> throw new IllegalStateException("Neznana kategorija: " + category);
        };
    }

    private double convertTemperature(String sourceUnit, String targetUnit, double value) {
        if (CELSIUS_UNIT.equals(sourceUnit) && FAHRENHEIT_UNIT.equals(targetUnit)) {
            return (value * 9.0 / 5.0) + 32.0;
        }
        if (FAHRENHEIT_UNIT.equals(sourceUnit) && CELSIUS_UNIT.equals(targetUnit)) {
            return (value - 32.0) * 5.0 / 9.0;
        }
        throw new IllegalStateException("Nepodprta temperaturna pretvorba: " + sourceUnit + " -> " + targetUnit);
    }

    private double convertLengthToMeters(String unit, double value) {
        return switch (unit) {
            case METER_UNIT -> value;
            case CENTIMETER_UNIT -> value / 100.0;
            case INCH_UNIT -> value * 0.0254;
            case FOOT_UNIT -> value * 0.3048;
            default -> throw new IllegalStateException("Nepodprta enota dolžine: " + unit);
        };
    }

    private double convertMetersToTargetUnit(String unit, double valueInMeters) {
        return switch (unit) {
            case METER_UNIT -> valueInMeters;
            case CENTIMETER_UNIT -> valueInMeters * 100.0;
            case INCH_UNIT -> valueInMeters / 0.0254;
            case FOOT_UNIT -> valueInMeters / 0.3048;
            default -> throw new IllegalStateException("Nepodprta enota dolžine: " + unit);
        };
    }

    private double convertMassToKilograms(String unit, double value) {
        return switch (unit) {
            case KILOGRAM_UNIT -> value;
            case GRAM_UNIT -> value / 1000.0;
            case OUNCE_UNIT -> value * 0.028349523125;
            default -> throw new IllegalStateException("Nepodprta enota mase: " + unit);
        };
    }

    private double convertKilogramsToTargetUnit(String unit, double valueInKilograms) {
        return switch (unit) {
            case KILOGRAM_UNIT -> valueInKilograms;
            case GRAM_UNIT -> valueInKilograms * 1000.0;
            case OUNCE_UNIT -> valueInKilograms / 0.028349523125;
            default -> throw new IllegalStateException("Nepodprta enota mase: " + unit);
        };
    }

    @FXML
    private void onOpenClick() {
        File selectedFile = chooseFileForOpen("Odpri zgodovino pretvorb", "Besedilne datoteke",
                TEXT_FILE_EXTENSION_PATTERN);
        if (selectedFile == null) {
            updateStatusAndLog("Odpiranje datoteke je bilo preklicano.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(selectedFile.toPath(), StandardCharsets.UTF_8);
            historyEntries.clear();

            int loadedEntries = 0;
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                HistoryEntry parsedEntry = parseHistoryLine(line);
                if (parsedEntry != null) {
                    historyEntries.add(parsedEntry);
                    loadedEntries++;
                }
            }

            renderHistoryTiles();
            expandOnlyPane(pane2);

            long fileSize = Files.size(selectedFile.toPath());
            updateStatusAndLog("Odprta datoteka: " + selectedFile.getName() + " (" + fileSize + " B), "
                    + loadedEntries + " zapisov.");
        } catch (IOException exception) {
            updateStatusAndLogError("Napaka pri odpiranju datoteke: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveClick() {
        File selectedFile = chooseFileForSave("Shrani zgodovino pretvorb", "Besedilne datoteke",
                TEXT_FILE_EXTENSION_PATTERN, null);
        if (selectedFile == null) {
            updateStatusAndLog("Shranjevanje datoteke je bilo preklicano.");
            return;
        }

        try {
            File targetFile = ensureHistoryFileExtension(selectedFile);
            Files.writeString(targetFile.toPath(), serializeHistoryEntries(), StandardCharsets.UTF_8);
            long fileSize = Files.size(targetFile.toPath());
            updateStatusAndLog("Shranjen zapis: " + targetFile.getName() + " (" + fileSize + " B).");
        } catch (IOException exception) {
            updateStatusAndLogError("Napaka pri shranjevanju datoteke: " + exception.getMessage());
        }
    }

    @FXML
    private void onOpenLogClick() {
        File selectedFile = chooseFileForOpen("Odpri dnevnik dogodkov", "Dnevniške datoteke (*.log)",
                LOG_FILE_EXTENSION_PATTERN);
        if (selectedFile == null) {
            updateStatusAndLog("Odpiranje dnevnika je bilo preklicano.");
            return;
        }

        try {
            List<String> loadedLogLines = Files.readAllLines(selectedFile.toPath(), StandardCharsets.UTF_8);
            expandOnlyPane(pane3);
            replaceEventLogContent(loadedLogLines);
            scrollEventLogToBottom();

            long fileSize = Files.size(selectedFile.toPath());
            updateStatusAndLog("Odprt dnevnik: " + selectedFile.getName() + " (" + fileSize + " B), "
                    + loadedLogLines.size() + " vrstic.");
        } catch (IOException exception) {
            updateStatusAndLogError("Napaka pri odpiranju dnevnika: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveLogClick() {
        File selectedFile = chooseFileForSave("Shrani dnevnik dogodkov", "Dnevniške datoteke (*.log)",
                LOG_FILE_EXTENSION_PATTERN, "dnevnik.log");
        if (selectedFile == null) {
            updateStatusAndLog("Shranjevanje dnevnika je bilo preklicano.");
            return;
        }

        try {
            File targetLogFile = ensureLogFileExtension(selectedFile);
            Files.writeString(targetLogFile.toPath(), serializeEventLogEntries(), StandardCharsets.UTF_8);
            long fileSize = Files.size(targetLogFile.toPath());
            updateStatusAndLog("Shranjen dnevnik: " + targetLogFile.getName() + " (" + fileSize + " B).");
        } catch (IOException exception) {
            updateStatusAndLogError("Napaka pri shranjevanju dnevnika: " + exception.getMessage());
        }
    }

    private File chooseFileForOpen(String title, String extensionDescription, String extensionPattern) {
        FileChooser fileChooser = createSingleExtensionFileChooser(title, extensionDescription, extensionPattern);
        return fileChooser.showOpenDialog(statusLabel.getScene().getWindow());
    }

    private File chooseFileForSave(String title, String extensionDescription, String extensionPattern,
            String initialFileName) {
        FileChooser fileChooser = createSingleExtensionFileChooser(title, extensionDescription, extensionPattern);
        if (initialFileName != null && !initialFileName.isBlank()) {
            fileChooser.setInitialFileName(initialFileName);
        }
        return fileChooser.showSaveDialog(statusLabel.getScene().getWindow());
    }

    private FileChooser createSingleExtensionFileChooser(String title, String extensionDescription,
            String extensionPattern) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extensionDescription, extensionPattern));
        return fileChooser;
    }

    private String serializeEventLogEntries() {
        return String.join(System.lineSeparator(), eventLogEntries);
    }

    private File ensureLogFileExtension(File selectedFile) {
        String selectedName = selectedFile.getName() + LOG_FILE_EXTENSION;
        String parentPath = selectedFile.getParent();
        if (parentPath == null || parentPath.isBlank()) {
            return new File(selectedName);
        }
        return new File(parentPath, selectedName);
    }

    private File ensureHistoryFileExtension(File selectedFile) {
        String selectedName = selectedFile.getName() + TEXT_FILE_EXTENSION;
        String parentPath = selectedFile.getParent();
        if (parentPath == null || parentPath.isBlank()) {
            return new File(selectedName);
        }
        return new File(parentPath, selectedName);
    }

    @FXML
    private void onClearLogClick() {
        eventLogEntries.clear();
        refreshVisibleEventLog();

        if (isStatusResetQueued) {
            applyNormalStatusStyle();
            isStatusResetQueued = false;
        }
        statusLabel.setText("Dnevnik je počiščen.");
    }

    @FXML
    private void onClearAllClick() {
        firstValueTextField.clear();
        secondValueTextField.clear();
        historyEntries.clear();
        renderHistoryTiles();
        updateStatusAndLog("Vnos in zgodovina pretvorb sta pobrisana.");
    }

    @FXML
    private void onAuthorClick() {
        updateStatusAndLog(AUTHOR_INFORMATION);
    }

    @FXML
    private void onAboutProgramClick() {
        updateStatusAndLog("Odpiram projektno stran...");

        Thread openLinkThread = new Thread(() -> {
            boolean opened = tryOpenProjectUrlWithXdgOpen();
            Platform.runLater(() -> {
                if (opened) {
                    updateStatusAndLog("Odprt projekt: " + PROJECT_URL);
                } else {
                    updateStatusAndLog("Povezave ni bilo mogoče odpreti samodejno. URL: " + PROJECT_URL);
                }
            });
        }, "about-program-link-opener");
        openLinkThread.setDaemon(true);
        openLinkThread.start();
    }

    private boolean tryOpenProjectUrlWithXdgOpen() {
        try {
            Process process = new ProcessBuilder("xdg-open", PROJECT_URL).start();
            return process.isAlive() || process.waitFor() == 0;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @FXML
    private void onExitClick() {
        appendEventLog("Aplikacija se zapira.");
        Platform.exit();
    }

    private void appendConversionToHistory(HistoryEntry historyEntry) {
        historyEntries.add(historyEntry);
        renderHistoryTiles();
        appendEventLog("Dodan zapis v zgodovino: " + historyEntry.getFirstDisplay() + " -> "
                + historyEntry.getSecondDisplay() + ".");
    }

    private String serializeHistoryEntries() {
        StringBuilder outputBuilder = new StringBuilder();
        outputBuilder.append(HISTORY_FILE_HEADER);

        for (HistoryEntry entry : historyEntries) {
            outputBuilder
                    .append(System.lineSeparator())
                    .append(entry.getFirstValueText())
                    .append(HISTORY_FILE_DELIMITER)
                    .append(entry.getFirstUnit())
                    .append(HISTORY_FILE_DELIMITER)
                    .append(entry.getSecondValueText())
                    .append(HISTORY_FILE_DELIMITER)
                    .append(entry.getSecondUnit());
        }

        return outputBuilder.toString();
    }

    private HistoryEntry parseHistoryLine(String line) {
        HistoryEntry entry = parseDelimitedHistoryLine(line);
        if (entry != null) {
            return entry;
        }

        appendEventLog("Preskočena neveljavna vrstica zgodovine: " + line);
        return null;
    }

    private HistoryEntry parseDelimitedHistoryLine(String line) {
        String[] parts = line.split(HISTORY_FILE_DELIMITER, -1);
        if (parts.length == 4) {
            return new HistoryEntry(
                    normalizeNumericText(parts[0].trim()),
                    parts[1].trim(),
                    normalizeNumericText(parts[2].trim()),
                    parts[3].trim());
        }
        return null;
    }

    private String normalizeNumericText(String numericText) {
        try {
            return formatNumber(Double.parseDouble(numericText.replace(',', '.')));
        } catch (NumberFormatException exception) {
            appendEventLog("Nepodprta številčna vrednost v zapisu zgodovine: " + numericText);
            return numericText;
        }
    }

    private String formatNumber(double numberValue) {
        return numberFormatter.format(numberValue);
    }

    private void updateStatusAndLog(String statusMessage) {
        if (isStatusResetQueued) {
            applyNormalStatusStyle();
            isStatusResetQueued = false;
        }
        statusLabel.setText(statusMessage);
        appendEventLog(statusMessage);
    }

    private void updateStatusAndLogError(String statusMessage) {
        applyErrorStatusStyle();
        isStatusResetQueued = true;
        statusLabel.setText(statusMessage);
        appendEventLog(statusMessage);
    }

    private void applyErrorStatusStyle() {
        if (!statusLabel.getStyleClass().contains(STATUS_ERROR_STYLE_CLASS)) {
            statusLabel.getStyleClass().add(STATUS_ERROR_STYLE_CLASS);
        }
    }

    private void applyNormalStatusStyle() {
        statusLabel.getStyleClass().remove(STATUS_ERROR_STYLE_CLASS);
    }

    private void appendEventLog(String logMessage) {
        String timestamp = LocalTime.now().format(timestampFormatter);
        String logEntry = "[" + timestamp + "] " + logMessage;
        appendLogEntryLine(logEntry);
    }

    private void renderHistoryTiles() {
        if (historyTilesContainer == null) {
            return;
        }

        historyTilesContainer.getChildren().clear();
        for (int index = historyEntries.size() - 1; index >= 0; index--) {
            addHistoryTile(historyEntries.get(index));
        }
    }

    private void addHistoryTile(HistoryEntry entry) {
        if (historyTilesContainer == null) {
            return;
        }

        HBox tileRow = new HBox(10);
        tileRow.getStyleClass().add("history-tile");

        Label firstValueLabel = new Label(entry.getFirstDisplay());
        firstValueLabel.getStyleClass().add("history-tile-value");

        Label arrowLabel = new Label("→");
        arrowLabel.getStyleClass().add("history-tile-arrow");

        Label secondValueLabel = new Label(entry.getSecondDisplay());
        secondValueLabel.getStyleClass().add("history-tile-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button restoreButton = new Button("Obnovi");
        restoreButton.getStyleClass().addAll("btn", "btn-default", "restore-history-button");
        restoreButton.setOnAction(event -> restoreHistoryEntry(entry));

        tileRow.getChildren().addAll(firstValueLabel, arrowLabel, secondValueLabel, spacer, restoreButton);
        historyTilesContainer.getChildren().add(tileRow);
    }

    private static final class HistoryEntry {
        private final String firstValueText;
        private final String firstUnit;
        private final String secondValueText;
        private final String secondUnit;

        private HistoryEntry(String firstValueText, String firstUnit, String secondValueText, String secondUnit) {
            this.firstValueText = firstValueText;
            this.firstUnit = firstUnit;
            this.secondValueText = secondValueText;
            this.secondUnit = secondUnit;
        }

        private String getFirstValueText() {
            return firstValueText;
        }

        private String getFirstUnit() {
            return firstUnit;
        }

        private String getSecondValueText() {
            return secondValueText;
        }

        private String getSecondUnit() {
            return secondUnit;
        }

        private String getFirstDisplay() {
            if (firstUnit == null || firstUnit.isBlank()) {
                return firstValueText;
            }
            return firstValueText + " " + firstUnit;
        }

        private String getSecondDisplay() {
            if (secondUnit == null || secondUnit.isBlank()) {
                return secondValueText;
            }
            return secondValueText + " " + secondUnit;
        }

        @Override
        public boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            }
            if (!(otherObject instanceof HistoryEntry otherEntry)) {
                return false;
            }
            return Objects.equals(firstValueText, otherEntry.firstValueText)
                    && Objects.equals(firstUnit, otherEntry.firstUnit)
                    && Objects.equals(secondValueText, otherEntry.secondValueText)
                    && Objects.equals(secondUnit, otherEntry.secondUnit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstValueText, firstUnit, secondValueText, secondUnit);
        }
    }

}
