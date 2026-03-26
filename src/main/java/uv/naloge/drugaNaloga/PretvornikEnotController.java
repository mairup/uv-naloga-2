package uv.naloge.drugaNaloga;

import java.awt.Desktop;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
import javafx.util.Duration;

public class PretvornikEnotController {

    private static final String LENGTH_CATEGORY = "Dolzina";
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
    private Button openToolbarButton;

    @FXML
    private Button saveToolbarButton;

    @FXML
    private Button clearToolbarButton;

    @FXML
    private Button exitToolbarButton;

    @FXML
    private Button directionToggleButton;

    @FXML
    private ScrollPane centerContentScrollPane;

    private final Map<String, List<String>> unitsByCategory = new LinkedHashMap<>();
    private final DecimalFormat numberFormatter = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance());
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final ObservableList<HistoryEntry> historyEntries = FXCollections.observableArrayList();
    private boolean isFirstToSecondConversion = true;

    public void initialize() {
        initializeCategoriesAndUnits();
        initializeToolbarIcons();
        renderHistoryTiles();
        updateDirectionToggleState();
        statusLabel.setText("Pripravljeno.");
        appendEventLog("Aplikacija je pripravljena za delo.");

        setupAccordionLogic();
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
                });
            }
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
            updateStatusAndLog("Napaka: vnos je prazen.");
            return;
        }

        String selectedCategory = categoryComboBox.getValue();
        String selectedSourceUnit = sourceUnitField.getValue();
        String selectedTargetUnit = targetUnitField.getValue();

        if (selectedCategory == null || selectedSourceUnit == null || selectedTargetUnit == null) {
            updateStatusAndLog("Napaka: kategorija ali enota ni izbrana.");
            return;
        }

        double inputNumericValue;
        try {
            inputNumericValue = Double.parseDouble(inputText);
        } catch (NumberFormatException exception) {
            updateStatusAndLog("Napaka pri vnosu: " + inputText + ".");
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
        appendConversionToHistory(historyEntry);
        updateStatusAndLog("Izvedena pretvorba: " + conversionEntry + ".");
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

        firstValueTextField.setText(entry.getFirstValueText());
        secondValueTextField.setText(entry.getSecondValueText());
        selectUnitIfAvailable(firstUnitComboBox, entry.getFirstUnit());
        selectUnitIfAvailable(secondUnitComboBox, entry.getSecondUnit());
        scrollConverterIntoView();

        updateStatusAndLog("Obnovljena pretvorba: " + entry.getFirstDisplay() + " -> " + entry.getSecondDisplay()
                + ".");
    }

    private void scrollConverterIntoView() {
        if (centerContentScrollPane == null) {
            return;
        }

        Runnable scrollAction = () -> Platform.runLater(() -> centerContentScrollPane.setVvalue(0.0));

        if (pane1 != null && !pane1.isExpanded()) {
            ChangeListener<Boolean> expansionListener = new ChangeListener<>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Boolean> observable,
                        Boolean oldValue, Boolean newValue) {
                    if (Boolean.TRUE.equals(newValue)) {
                        pane1.expandedProperty().removeListener(this);
                        runDelayedScroll(scrollAction);
                    }
                }
            };
            pane1.expandedProperty().addListener(expansionListener);
            return;
        }

        runDelayedScroll(scrollAction);
    }

    private void runDelayedScroll(Runnable scrollAction) {
        PauseTransition pauseTransition = new PauseTransition(Duration.millis(350));
        pauseTransition.setOnFinished(event -> scrollAction.run());
        pauseTransition.play();
    }

    private void selectUnitIfAvailable(ComboBox<String> comboBox, String unit) {
        if (comboBox.getItems().contains(unit)) {
            comboBox.getSelectionModel().select(unit);
        }
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
                    updateStatusAndLog("Povezave ni bilo mogoce odpreti samodejno. URL: " + PROJECT_URL);
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
        addHistoryTile(historyEntry);
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
            appendEventLog("Preskocena neveljavna vrstica zgodovine: " + line);
            return null;
        }

        String[] legacyParts = line.split("->");
        if (legacyParts.length != 2) {
            appendEventLog("Preskocena neveljavna vrstica zgodovine: " + line);
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
        statusLabel.setText(statusMessage);
        appendEventLog(statusMessage);
    }

    private void appendEventLog(String logMessage) {
        String timestamp = LocalTime.now().format(timestampFormatter);
        String logEntry = "[" + timestamp + "] " + logMessage;
        if (eventLogTextArea.getText().isBlank()) {
            eventLogTextArea.setText(logEntry);
            return;
        }
        eventLogTextArea.appendText(System.lineSeparator() + logEntry);
    }

    private void renderHistoryTiles() {
        if (historyTilesContainer == null) {
            return;
        }

        historyTilesContainer.getChildren().clear();
        for (HistoryEntry entry : historyEntries) {
            addHistoryTile(entry);
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
    }

}
