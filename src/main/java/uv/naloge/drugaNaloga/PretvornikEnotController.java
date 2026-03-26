package uv.naloge.drugaNaloga;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
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

    @FXML
    private TextField inputValueTextField;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private ComboBox<String> sourceUnitComboBox;

    @FXML
    private ComboBox<String> targetUnitComboBox;

    @FXML
    private TextArea conversionHistoryTextArea;

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

    private final Map<String, List<String>> unitsByCategory = new LinkedHashMap<>();
    private final DecimalFormat numberFormatter = new DecimalFormat("0.########", DecimalFormatSymbols.getInstance());
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void initialize() {
        initializeCategoriesAndUnits();
        initializeToolbarIcons();
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

        sourceUnitComboBox.getItems().setAll(units);
        targetUnitComboBox.getItems().setAll(units);

        if (!units.isEmpty()) {
            sourceUnitComboBox.getSelectionModel().selectFirst();
            targetUnitComboBox.getSelectionModel().select(Math.min(1, units.size() - 1));
        }
    }

    @FXML
    private void onNumericKeypadButtonClick(ActionEvent actionEvent) {
        Object userDataValue = ((Button) actionEvent.getSource()).getUserData();
        if (userDataValue == null) {
            return;
        }

        String inputAppendix = userDataValue.toString();
        String currentInputValue = inputValueTextField.getText();
        if (".".equals(inputAppendix) && currentInputValue.contains(".")) {
            return;
        }
        inputValueTextField.setText(currentInputValue + inputAppendix);
    }

    @FXML
    private void onClearInputClick() {
        inputValueTextField.clear();
        updateStatusAndLog("Vnos v pretvorniku je bil pobrisan.");
    }

    @FXML
    private void onConvertClick() {
        String inputText = inputValueTextField.getText().trim().replace(',', '.');
        if (inputText.isEmpty()) {
            updateStatusAndLog("Napaka: vnos je prazen.");
            return;
        }

        String selectedCategory = categoryComboBox.getValue();
        String selectedSourceUnit = sourceUnitComboBox.getValue();
        String selectedTargetUnit = targetUnitComboBox.getValue();

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
        String conversionInput = formatNumber(inputNumericValue) + " " + selectedSourceUnit;
        String conversionOutput = formatNumber(convertedValue) + " " + selectedTargetUnit;
        String conversionEntry = conversionInput + " -> " + conversionOutput;

        inputValueTextField.setText(formatNumber(convertedValue));
        appendConversionToHistory(conversionEntry);
        updateStatusAndLog("Izvedena pretvorba: " + conversionEntry + ".");
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
            String fileContent = Files.readString(selectedFile.toPath(), StandardCharsets.UTF_8);
            conversionHistoryTextArea.setText(fileContent);
            long fileSize = Files.size(selectedFile.toPath());
            updateStatusAndLog("Odprta datoteka: " + selectedFile.getName() + " (" + fileSize + " B).");
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
            Files.writeString(selectedFile.toPath(), conversionHistoryTextArea.getText(), StandardCharsets.UTF_8);
            long fileSize = Files.size(selectedFile.toPath());
            updateStatusAndLog("Shranjen zapis: " + selectedFile.getName() + " (" + fileSize + " B).");
        } catch (IOException exception) {
            updateStatusAndLog("Napaka pri shranjevanju datoteke: " + exception.getMessage());
        }
    }

    @FXML
    private void onClearAllClick() {
        inputValueTextField.clear();
        conversionHistoryTextArea.clear();
        updateStatusAndLog("Vnos in zgodovina pretvorb sta pobrisana.");
    }

    @FXML
    private void onAuthorClick() {
        updateStatusAndLog(AUTHOR_INFORMATION);
    }

    @FXML
    private void onExitClick() {
        appendEventLog("Aplikacija se zapira.");
        Platform.exit();
    }

    private void appendConversionToHistory(String historyEntry) {
        if (conversionHistoryTextArea.getText().isBlank()) {
            conversionHistoryTextArea.setText(historyEntry);
            return;
        }
        conversionHistoryTextArea.appendText(System.lineSeparator() + historyEntry);
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

}
