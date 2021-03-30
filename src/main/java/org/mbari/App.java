package org.mbari;

import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Stage;
import java.time.Duration;
import java.util.Collections;

import org.mbari.vcr4j.sharktopoda.client.localization.IO;
import org.mbari.vcr4j.sharktopoda.client.localization.Localization;

/**
 * JavaFX App
 */
public class App extends Application {

    private IO io;

    @Override
    public void start(Stage stage) {


        var table = new TableView<Localization>();
        table.setEditable(false);

        var conceptCol = new TableColumn<Localization, String>("Concept");
        conceptCol.setCellValueFactory(new PropertyValueFactory<Localization, String>("concept"));


        var timeCol = new TableColumn<Localization, Duration>("ElapsedTime");
        timeCol.setCellValueFactory(new PropertyValueFactory<Localization, Duration>("elapsedTime"));
        timeCol.setCellFactory(column -> {
                return new TableCell<Localization, Duration>() {
                    @Override
                    protected void updateItem(Duration item, boolean empty) {
                        if (item == null || empty) {
                            setText(null);
                        }
                        else {
                            setText(formatDuration(item));
                        }
                    }
                };
            });

        table.getColumns().addAll(timeCol, conceptCol);

        var scene = new Scene(table, 640, 480);
        stage.setScene(scene);
        stage.show();

        initComms(table);
    }

    private String formatDuration(Duration duration) {
        return String.format("%d:%02d:%02d:%03d",
                                duration.toHours(),
                                duration.toMinutesPart(),
                                duration.toSecondsPart(),
                                duration.toMillisPart());
    }

    private void initComms(TableView<Localization> table) {
        var incomingPort = 5561;   // ZeroMQ subscriber port
        var outgoingPort = 5562;   // ZeroMQ publisher port
        var incomingTopic = "localization";
        var outgoingTopic = "localization";
        io = new IO(outgoingPort, incomingPort, outgoingTopic, incomingTopic);

        var items = io.getController().getLocalizations();
        items.addListener((ListChangeListener.Change<? extends Localization> c) -> {
            System.out.println("Received: " + c);
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(loc -> System.out.println(loc));
                }
            }
        });
        table.setItems(items);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            System.out.println("newSelection: " + newSelection);
            if (newSelection != null) {
                io.getSelectionController().select(Collections.singleton(newSelection), true);
            }
            else {
                io.getSelectionController().clearSelections();
            }
        });
    }

    public static void main(String[] args) {
        launch();
    }

}
