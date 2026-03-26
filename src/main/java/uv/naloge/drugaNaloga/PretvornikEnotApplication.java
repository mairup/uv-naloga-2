package uv.naloge.drugaNaloga;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;
import java.util.Objects;

public class PretvornikEnotApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                PretvornikEnotApplication.class.getResource("pretvornik-enot-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 760);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        scene.getStylesheets().add(
                Objects.requireNonNull(PretvornikEnotApplication.class.getResource("styles.css")).toExternalForm());
        stage.setTitle("Pretvornik enot");
        stage.setScene(scene);
        stage.show();
    }
}
