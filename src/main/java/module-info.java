module uv.naloge.druga.druganaloga {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens uv.naloge.druga.druganaloga to javafx.fxml;
    exports uv.naloge.druga.druganaloga;
}