package uv.naloge.drugaNaloga;

import java.awt.Desktop;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final String AUTHOR_INFORMATION = "Avtor: Mai Rupnik, 2. letnik, BVS-RI @ FRI, UNI-LJ";
    private static final String PROJECT_URL = "https://github.com/mairup/uv-naloga-2";
    private static final String HISTORY_FILE_DELIMITER = "\t";
    private static final String LOG_FILE_EXTENSION = ".log";
    private static final String STATUS_ERROR_STYLE_CLASS = "status-label-error";
    private static final double MIN_EXPANDED_PANE_CONTENT_HEIGHT = 80;
    private static final double ACCORDION_VERTICAL_SPACING = 10;
    private static final int MIN_VISIBLE_LOG_LINES = 2;
    private static final double LOG_LINE_HEIGHT_ESTIMATE = 18;
    private static final double LOG_CONTENT_VERTICAL_PADDING = 18;
    private static final double LOG_CHARACTER_WIDTH_ESTIMATE = 7.2;
    private static final double LOG_HORIZONTAL_PADDING_ESTIMATE = 24;
    private static final int MIN_VISIBLE_LOG_CHARACTERS = 32;
    private static final String TRUNCATION_SUFFIX = "...";

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
    private VBox accordionContainer;

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
    private final StringBuilder completeEventLogContent = new StringBuilder();
    private boolean isFirstToSecondConversion = true;
    private boolean isStatusResetQueued;

    public void initialize() {
        initializeCategoriesAndUnits();
        initializeToolbarIcons();
        renderHistoryTiles();
        updateDirectionToggleState();
        statusLabel.setText("Pripravljeno.");
        appendEventLog("Aplikacija je pripravljena za delo.");

        setupAccordionLogic();
        setupDynamicPaneHeightManagement();
        requestAccordionPaneHeightRefresh();
    }

    private void setupAccordionLogic() {
        TitledPane[] panes = { pane1, pane2, pane3 };
        for (TitledPane pane : panes) {
            if (pane != null) {
                pane.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                    if (isNowExpanded) {
                        for (TitledPane other : panes) {
                            if (other != pane && other != null) {
                                other.setExpanded(false);
                            }
                        }
                    }
                    requestAccordionPaneHeightRefresh();
                });
            }
        }
    }

    private void setupDynamicPaneHeightManagement() {
        if (eventLogTextArea != null) {
            eventLogTextArea.setPrefHeight(Region.USE_COMPUTED_SIZE);
            eventLogTextArea.setMinHeight(Region.USE_COMPUTED_SIZE);
            eventLogTextArea.setMaxHeight(Double.MAX_VALUE);
            eventLogTextArea.heightProperty().addListener((obs, oldHeight, newHeight) -> refreshVisibleEventLog());
            eventLogTextArea.widthProperty().addListener((obs, oldWidth, newWidth) -> refreshVisibleEventLog());
        }

        if (historyScrollPane != null) {
            historyScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
            historyScrollPane.setMinHeight(MIN_EXPANDED_PANE_CONTENT_HEIGHT);
            historyScrollPane.setMaxHeight(Double.MAX_VALUE);
        }

        if (accordionContainer != null) {
            accordionContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.heightProperty()
                            .addListener((sceneObs, oldHeight, newHeight) -> requestAccordionPaneHeightRefresh());
                    newScene.windowProperty().addListener((windowObs, oldWindow, newWindow) -> {
                        if (newWindow != null) {
                            newWindow.heightProperty().addListener(
                                    (heightObs, oldHeight, newHeight) -> requestAccordionPaneHeightRefresh());
                        }
                    });
                }
            });
            accordionContainer.heightProperty()
                    .addListener((obs, oldHeight, newHeight) -> requestAccordionPaneHeightRefresh());
        }
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

    @FXML
    private void onCategoryChanged() {
        updateUnitsForSelectedCategory();
        String selectedCategory = categoryComboBox.getValue();
        updateStatusAndLog("Izbrana kategorija: " + selectedCategory + ".");
    }

    private void updateUnitsForSelectedCategory() {
        String selectedCategory = categoryComboBox.getValue();
        List<String> units = unitsByCategory.getOrDefault(selectedCategory, List.of());

        firstUnitComboBox.getItems().setAll(units);
        secondUnitComboBox.getItems().setAll(units);

        if (!units.isEmpty()) {
            firstUnitComboBox.getSelectionModel().selectFirst();
            secondUnitComboBox.getSelectionModel().select(Math.min(1, units.size() - 1));
        }
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

        double inputNumericValue;
        try {
            inputNumericValue = Double.parseDouble(inputText);
        } catch (NumberFormatException exception) {
            updateStatusAndLogError("Napaka pri vnosu: " + inputText + ".");
            return;
        }

        double convertedValue = convertValue(selectedCategory, selectedSourceUnit, selectedTargetUnit,
                inputNumericValue);
        String sourceValueText = formatNumber(inputNumericValue);
        String targetValueText = formatNumber(convertedValue);

        HistoryEntry historyEntry = new HistoryEntry(
                sourceValueText,
                selectedSourceUnit,
                targetValueText,
                selectedTargetUnit);
        String conversionEntry = historyEntry.getFirstDisplay() + " -> " + historyEntry.getSecondDisplay();

        targetValueField.setText(targetValueText);
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
        return currentEntry.getFirstValueText().equals(lastEntry.getFirstValueText())
                && currentEntry.getFirstUnit().equals(lastEntry.getFirstUnit())
                && currentEntry.getSecondValueText().equals(lastEntry.getSecondValueText())
                && currentEntry.getSecondUnit().equals(lastEntry.getSecondUnit());
    }

    private void restoreHistoryEntry(HistoryEntry entry) {
        if (pane1 != null && !pane1.isExpanded()) {
            pane1.setExpanded(true);
            appendEventLog("Ob obnovitvi je bil odprt razdelek Pretvornik enot.");
        }

        if (!isFirstToSecondConversion) {
            isFirstToSecondConversion = true;
            updateDirectionToggleState();
            appendEventLog("Smer pretvorbe je bila ob obnovitvi nastavljena navzdol.");
        }

        String targetCategory = getCategoryForUnit(entry.getFirstUnit());
        if (targetCategory != null && !targetCategory.equals(categoryComboBox.getValue())) {
            categoryComboBox.getSelectionModel().select(targetCategory);
            updateUnitsForSelectedCategory();
        }

        firstValueTextField.setText(entry.getFirstValueText());
        secondValueTextField.setText(entry.getSecondValueText());
        selectUnitIfAvailable(firstUnitComboBox, entry.getFirstUnit());
        selectUnitIfAvailable(secondUnitComboBox, entry.getSecondUnit());
        scrollConverterIntoView();

        updateStatusAndLog("Obnovljena pretvorba: " + entry.getFirstDisplay() + " -> " + entry.getSecondDisplay()
                + ".");
    }

    private void scrollConverterIntoView() {
        if (pane1 != null && !pane1.isExpanded()) {
            pane1.setExpanded(true);
        }
    }

    private void requestAccordionPaneHeightRefresh() {
        Platform.runLater(this::refreshAccordionPaneHeights);
    }

    private void refreshAccordionPaneHeights() {
        double maxExpandedContentHeight = computeExpandedPaneContentMaxHeight();

        if (pane2 != null && pane2.isExpanded()) {
            Region historyContentRegion = getHistoryContentRegion();
            if (historyContentRegion != null) {
                double preferredHeight = maxExpandedContentHeight;
                applyDynamicContentHeight(historyContentRegion, preferredHeight, maxExpandedContentHeight);
            }
        }

        if (pane3 != null && pane3.isExpanded() && eventLogTextArea != null) {
            double preferredHeight = maxExpandedContentHeight;
            applyDynamicContentHeight(eventLogTextArea, preferredHeight, maxExpandedContentHeight);
            refreshVisibleEventLog();
        }
    }

    private Region getHistoryContentRegion() {
        return historyScrollPane;
    }

    private double computeExpandedPaneContentMaxHeight() {
        if (accordionContainer == null) {
            return Double.MAX_VALUE;
        }

        double totalHeight = accordionContainer.getHeight();
        if (totalHeight <= 0) {
            return Double.MAX_VALUE;
        }

        double headerHeights = resolvePaneTitleHeight(pane1) + resolvePaneTitleHeight(pane2)
                + resolvePaneTitleHeight(pane3);
        double spacingHeights = ACCORDION_VERTICAL_SPACING * 2;
        double availableHeight = totalHeight - headerHeights - spacingHeights;
        return Math.max(MIN_EXPANDED_PANE_CONTENT_HEIGHT, availableHeight);
    }

    private double resolvePaneTitleHeight(TitledPane pane) {
        if (pane == null) {
            return 0;
        }
        Node titleNode = pane.lookup(".title");
        if (titleNode != null) {
            return titleNode.getBoundsInLocal().getHeight();
        }
        return 40;
    }

    private void applyDynamicContentHeight(Region contentRegion, double preferredHeight, double maxHeight) {
        if (contentRegion == null) {
            return;
        }

        double safeMaxHeight = maxHeight > 0 ? maxHeight : MIN_EXPANDED_PANE_CONTENT_HEIGHT;
        double safePreferredHeight = Math.max(MIN_EXPANDED_PANE_CONTENT_HEIGHT, preferredHeight);
        double clampedHeight = Math.min(safePreferredHeight, safeMaxHeight);

        contentRegion.setMinHeight(Region.USE_COMPUTED_SIZE);
        contentRegion.setPrefHeight(clampedHeight);
        contentRegion.setMaxHeight(safeMaxHeight);
    }

    private int calculateVisibleLogLineCount() {
        if (eventLogTextArea == null) {
            return MIN_VISIBLE_LOG_LINES;
        }

        double availableHeight = eventLogTextArea.getHeight();
        if (availableHeight <= 0) {
            availableHeight = eventLogTextArea.prefHeight(-1);
        }

        double usableHeight = Math.max(0, availableHeight - LOG_CONTENT_VERTICAL_PADDING);
        int estimatedLineCount = (int) Math.floor(usableHeight / LOG_LINE_HEIGHT_ESTIMATE);
        return Math.max(MIN_VISIBLE_LOG_LINES, estimatedLineCount);
    }

    private int calculateVisibleLogCharacterLimit() {
        if (eventLogTextArea == null) {
            return 120;
        }

        double availableWidth = eventLogTextArea.getWidth();
        if (availableWidth <= 0) {
            availableWidth = eventLogTextArea.prefWidth(-1);
        }

        double usableWidth = Math.max(80, availableWidth - LOG_HORIZONTAL_PADDING_ESTIMATE);
        int estimatedCharacterCount = (int) Math.floor(usableWidth / LOG_CHARACTER_WIDTH_ESTIMATE);
        return Math.max(MIN_VISIBLE_LOG_CHARACTERS, estimatedCharacterCount);
    }

    private String truncateLogLineForDisplay(String line, int maxCharacters) {
        if (line == null) {
            return "";
        }
        if (line.length() <= maxCharacters) {
            return line;
        }

        int safeTextLength = Math.max(1, maxCharacters - TRUNCATION_SUFFIX.length());
        return line.substring(0, safeTextLength) + TRUNCATION_SUFFIX;
    }

    private void refreshVisibleEventLog() {
        if (eventLogTextArea == null) {
            return;
        }

        if (eventLogEntries.isEmpty()) {
            eventLogTextArea.clear();
            return;
        }

        int visibleLineCount = calculateVisibleLogLineCount();
        int visibleCharacterLimit = calculateVisibleLogCharacterLimit();
        int startIndex = Math.max(0, eventLogEntries.size() - visibleLineCount);

        StringBuilder visibleContentBuilder = new StringBuilder();
        for (int index = eventLogEntries.size() - 1; index >= startIndex; index--) {
            if (visibleContentBuilder.length() > 0) {
                visibleContentBuilder.append(System.lineSeparator());
            }
            String sourceLine = eventLogEntries.get(index);
            visibleContentBuilder.append(truncateLogLineForDisplay(sourceLine, visibleCharacterLimit));
        }

        eventLogTextArea.setText(visibleContentBuilder.toString());
        eventLogTextArea.positionCaret(0);
        eventLogTextArea.deselect();
    }

    private void rebuildCompleteEventLogContent() {
        completeEventLogContent.setLength(0);
        for (int index = 0; index < eventLogEntries.size(); index++) {
            if (index > 0) {
                completeEventLogContent.append(System.lineSeparator());
            }
            completeEventLogContent.append(eventLogEntries.get(index));
        }
    }

    private void replaceEventLogContent(List<String> loadedLogLines) {
        eventLogEntries.clear();
        eventLogEntries.addAll(loadedLogLines);
        rebuildCompleteEventLogContent();
        refreshVisibleEventLog();
    }

    private void appendLogEntryLine(String entryLine) {
        eventLogEntries.add(entryLine);
        if (completeEventLogContent.length() > 0) {
            completeEventLogContent.append(System.lineSeparator());
        }
        completeEventLogContent.append(entryLine);
        refreshVisibleEventLog();
    }

    private void selectUnitIfAvailable(ComboBox<String> comboBox, String unit) {
        if (comboBox.getItems().contains(unit)) {
            comboBox.getSelectionModel().select(unit);
        }
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

        if (LENGTH_CATEGORY.equals(category)) {
            double valueInMeters = convertLengthToMeters(sourceUnit, value);
            return convertMetersToTargetUnit(targetUnit, valueInMeters);
        }

        if (TEMPERATURE_CATEGORY.equals(category)) {
            if (CELSIUS_UNIT.equals(sourceUnit) && FAHRENHEIT_UNIT.equals(targetUnit)) {
                return (value * 9.0 / 5.0) + 32.0;
            }
            if (FAHRENHEIT_UNIT.equals(sourceUnit) && CELSIUS_UNIT.equals(targetUnit)) {
                return (value - 32.0) * 5.0 / 9.0;
            }
        }

        if (MASS_CATEGORY.equals(category)) {
            double valueInKilograms = convertMassToKilograms(sourceUnit, value);
            return convertKilogramsToTargetUnit(targetUnit, valueInKilograms);
        }

        return value;
    }

    private double convertLengthToMeters(String unit, double value) {
        if (METER_UNIT.equals(unit)) {
            return value;
        }
        if (CENTIMETER_UNIT.equals(unit)) {
            return value / 100.0;
        }
        if (INCH_UNIT.equals(unit)) {
            return value * 0.0254;
        }
        if (FOOT_UNIT.equals(unit)) {
            return value * 0.3048;
        }
        return value;
    }

    private double convertMetersToTargetUnit(String unit, double valueInMeters) {
        if (METER_UNIT.equals(unit)) {
            return valueInMeters;
        }
        if (CENTIMETER_UNIT.equals(unit)) {
            return valueInMeters * 100.0;
        }
        if (INCH_UNIT.equals(unit)) {
            return valueInMeters / 0.0254;
        }
        if (FOOT_UNIT.equals(unit)) {
            return valueInMeters / 0.3048;
        }
        return valueInMeters;
    }

    private double convertMassToKilograms(String unit, double value) {
        if (KILOGRAM_UNIT.equals(unit)) {
            return value;
        }
        if (GRAM_UNIT.equals(unit)) {
            return value / 1000.0;
        }
        if (OUNCE_UNIT.equals(unit)) {
            return value * 0.028349523125;
        }
        return value;
    }

    private double convertKilogramsToTargetUnit(String unit, double valueInKilograms) {
        if (KILOGRAM_UNIT.equals(unit)) {
            return valueInKilograms;
        }
        if (GRAM_UNIT.equals(unit)) {
            return valueInKilograms * 1000.0;
        }
        if (OUNCE_UNIT.equals(unit)) {
            return valueInKilograms / 0.028349523125;
        }
        return valueInKilograms;
    }

    @FXML
    private void onOpenClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Odpri zgodovino pretvorb");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Besedilne datoteke", "*.txt"));

        File selectedFile = fileChooser.showOpenDialog(statusLabel.getScene().getWindow());
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

            long fileSize = Files.size(selectedFile.toPath());
            updateStatusAndLog("Odprta datoteka: " + selectedFile.getName() + " (" + fileSize + " B), "
                    + loadedEntries + " zapisov.");
        } catch (IOException exception) {
            updateStatusAndLog("Napaka pri odpiranju datoteke: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Shrani zgodovino pretvorb");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Besedilne datoteke", "*.txt"));

        File selectedFile = fileChooser.showSaveDialog(statusLabel.getScene().getWindow());
        if (selectedFile == null) {
            updateStatusAndLog("Shranjevanje datoteke je bilo preklicano.");
            return;
        }

        try {
            Files.writeString(selectedFile.toPath(), serializeHistoryEntries(), StandardCharsets.UTF_8);
            long fileSize = Files.size(selectedFile.toPath());
            updateStatusAndLog("Shranjen zapis: " + selectedFile.getName() + " (" + fileSize + " B).");
        } catch (IOException exception) {
            updateStatusAndLog("Napaka pri shranjevanju datoteke: " + exception.getMessage());
        }
    }

    @FXML
    private void onOpenLogClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Odpri dnevnik dogodkov");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Dnevniške datoteke (*.log)", "*.log"));

        File selectedFile = fileChooser.showOpenDialog(statusLabel.getScene().getWindow());
        if (selectedFile == null) {
            updateStatusAndLog("Odpiranje dnevnika je bilo preklicano.");
            return;
        }

        try {
            List<String> loadedLogLines = Files.readAllLines(selectedFile.toPath(), StandardCharsets.UTF_8);
            replaceEventLogContent(loadedLogLines);

            if (pane3 != null && !pane3.isExpanded()) {
                pane3.setExpanded(true);
            }
            requestAccordionPaneHeightRefresh();

            long fileSize = Files.size(selectedFile.toPath());
            updateStatusAndLog("Odprt dnevnik: " + selectedFile.getName() + " (" + fileSize + " B), "
                    + loadedLogLines.size() + " vrstic.");
        } catch (IOException exception) {
            updateStatusAndLogError("Napaka pri odpiranju dnevnika: " + exception.getMessage());
        }
    }

    @FXML
    private void onSaveLogClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Shrani dnevnik dogodkov");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Dnevniške datoteke (*.log)", "*.log"));
        fileChooser.setInitialFileName("dnevnik.log");

        File selectedFile = fileChooser.showSaveDialog(statusLabel.getScene().getWindow());
        if (selectedFile == null) {
            updateStatusAndLog("Shranjevanje dnevnika je bilo preklicano.");
            return;
        }

        try {
            File targetLogFile = ensureLogFileExtension(selectedFile);
            Files.writeString(targetLogFile.toPath(), completeEventLogContent.toString(), StandardCharsets.UTF_8);
            long fileSize = Files.size(targetLogFile.toPath());
            updateStatusAndLog("Shranjen dnevnik: " + targetLogFile.getName() + " (" + fileSize + " B).");
        } catch (IOException exception) {
            updateStatusAndLogError("Napaka pri shranjevanju dnevnika: " + exception.getMessage());
        }
    }

    private File ensureLogFileExtension(File selectedFile) {
        if (selectedFile == null) {
            return null;
        }

        String selectedName = selectedFile.getName();
        if (selectedName.toLowerCase().endsWith(LOG_FILE_EXTENSION)) {
            return selectedFile;
        }

        String parentPath = selectedFile.getParent();
        if (parentPath == null || parentPath.isBlank()) {
            return new File(selectedName + LOG_FILE_EXTENSION);
        }
        return new File(parentPath, selectedName + LOG_FILE_EXTENSION);
    }

    @FXML
    private void onClearLogClick() {
        eventLogEntries.clear();
        completeEventLogContent.setLength(0);
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
            boolean opened = tryOpenProjectUrlWithDesktop() || tryOpenProjectUrlWithXdgOpen();
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

    private boolean tryOpenProjectUrlWithDesktop() {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return false;
        }
        try {
            Desktop.getDesktop().browse(URI.create(PROJECT_URL));
            return true;
        } catch (IOException exception) {
            return false;
        }
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
        outputBuilder.append("# firstValue\tfirstUnit\tsecondValue\tsecondUnit");

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
        String[] parts = line.split(HISTORY_FILE_DELIMITER);
        if (parts.length >= 4) {
            return new HistoryEntry(
                    normalizeNumericText(parts[0].trim()),
                    parts[1].trim(),
                    normalizeNumericText(parts[2].trim()),
                    parts[3].trim());
        }

        if (!line.contains("->")) {
            appendEventLog("Preskočena neveljavna vrstica zgodovine: " + line);
            return null;
        }

        String[] legacyParts = line.split("->");
        if (legacyParts.length != 2) {
            appendEventLog("Preskočena neveljavna vrstica zgodovine: " + line);
            return null;
        }

        String[] firstSide = splitLegacyDisplayPart(legacyParts[0].trim());
        String[] secondSide = splitLegacyDisplayPart(legacyParts[1].trim());
        return new HistoryEntry(
                normalizeNumericText(firstSide[0]),
                firstSide[1],
                normalizeNumericText(secondSide[0]),
                secondSide[1]);
    }

    private String normalizeNumericText(String numericText) {
        try {
            return formatNumber(Double.parseDouble(numericText.replace(',', '.')));
        } catch (NumberFormatException exception) {
            return numericText;
        }
    }

    private String[] splitLegacyDisplayPart(String displayPart) {
        int lastSpaceIndex = displayPart.lastIndexOf(' ');
        if (lastSpaceIndex <= 0 || lastSpaceIndex >= displayPart.length() - 1) {
            return new String[] { displayPart, "" };
        }

        String numericPart = displayPart.substring(0, lastSpaceIndex).trim();
        String unitPart = displayPart.substring(lastSpaceIndex + 1).trim();
        return new String[] { numericPart, unitPart };
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
        requestAccordionPaneHeightRefresh();
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
    }

}
