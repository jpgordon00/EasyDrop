import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.FileRequestListener;
import net.packet.FileSendRequestPacket;
import util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for 'MainScreen.java'.
 * This class handles many of the UI functions.
 * Responds to client networks and decrypts the file given the packet key.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/9/19
 **/
public class MainScreenC {


    private static long MIN_TOGGLE_TIME = 500;

    /**
     * Stage given during this controller's creation in 'MainScreen.java'.
     */
    public Stage primaryStage;
    /**
     * Node's loaded from 'Send Screen'.
     */
    public AnchorPane sendContent;
    public Button buttonSend;
    public Button buttonClear;
    public TextField pinField;
    public Label pinLabelResponse;
    public AnchorPane anchorContent;
    public Button buttonSelect, buttonSelectDir;
    public Rectangle rectDragDrop;
    public QueView queView;

    /**
     * Node's loaded for the receive Screen.
     */
    @FXML
    public TableView<TableItem> table;
    public TableColumn<TableItem, String> rColumnName;
    public TableColumn<TableItem, String> rColumnType;
    public TableColumn<TableItem, String> rColumnSize;
    public TableColumn<TableItem, String> rColumnTime;
    public Button buttonNav;
    @FXML
    public TextField textFieldSearch;
    @FXML
    public Button buttonExit, buttonMin, buttonAccAll, buttonDelAll;
    @FXML
    public Rectangle rectTop;
    @FXML
    public AnchorPane anchorPane, topPane;
    @FXML
    public Label titleLabel;

    @FXML
    public StackPane stackPane;

    /**
     * PIN assigned by BinClient.
     */
    public String PIN = "";

    /**
     *  choosers for our send screen.
     */
    public FileChooser chooser;
    public DirectoryChooser chooserDir;
    /**
     * Holds our current screen location to allow a draggable window.
     */
    private double xOffset = 0, yOffset = 0;
    /**
     * Handles the open/close of the send screen.
     * Times are represented in miliseconds.
     */
    private int sendHideY = 1000;
    private int sendShowY = -49;
    private boolean shown = false;
    private long lastToggle = 0;

    /**
     * Function that's called when the view is created.
     */
    @FXML
    private void initialize() {
        //load the receive content
        loadReceive();
    }

    /**
     * Shows/Hides the send screen.
     * All content will remain in memory.
     * A transition animation is played for 'sendContent', which slides UP from the BOTTOM.
     * A fade animation is played for some buttons on the top screen.
     * Only the following buttons on the top pane are retained:
     * buttonMin, buttonMax, titleLabel
     */
    public void toggleSend() {
        stackPane.setLayoutY(48);
        if (lastToggle == 0) {
            lastToggle = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - lastToggle <= MIN_TOGGLE_TIME) return;
            lastToggle = System.currentTimeMillis();
        }
        //slide up
        TranslateTransition trans = new TranslateTransition(Duration.millis(MainScreen.NAV_DURATION), sendContent);
        trans.setFromY(shown ? sendShowY : sendHideY);
        trans.setToY(shown ? sendHideY : sendShowY);
        trans.play();
        //fade buttons
        int k = !shown ? fadeOut(buttonAccAll, buttonDelAll, textFieldSearch) : fadeOut(buttonSend, buttonClear, pinField, pinLabelResponse);
        k = !shown ? fadeIn(buttonSend, buttonClear, pinField, pinLabelResponse) : fadeIn(buttonAccAll, buttonDelAll, textFieldSearch);
        shown = !shown;
    }

    /**
     * Fades out all given nodes from an opacity of 1.0 to 0.0.
     * Duration is NAV_FADE_DURATION, node is removed on anim. finish.
     * @param nodes to fade out.
     * @return value is for the ternary operation.
     */
    private int fadeOut(Node ... nodes) {
        for (Node node: nodes) {
            node.setOpacity(1.0);
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(MainScreen.NAV_FADE_DURATION), node);
            fadeTransition.setFromValue(1.0);
            fadeTransition.setToValue(0.0);
            fadeTransition.setOnFinished(event -> topPane.getChildren().remove(node));
            fadeTransition.play();
        }
        return 0;
    }

    /**
     * Fades in all given nodes from an opacity of 1.0 to 0.0.
     * Duration is NAV_FADE_DURATION, node is added before anim. begins.
     * @param nodes to fade in.
     * @return
     */
    private int fadeIn(Node ... nodes) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = () -> {
            Runnable r = () -> {
                for (Node node: nodes) {
                    node.setOpacity(0.0);
                    topPane.getChildren().add(node);
                    FadeTransition fadeTransition = new FadeTransition(Duration.millis(MainScreen.NAV_FADE_DURATION), node);
                    fadeTransition.setFromValue(0.0);
                    fadeTransition.setToValue(1.0);
                    fadeTransition.play();
                }
            };
            Platform.runLater(r);
        };
        scheduler.schedule(task, MainScreen.NAV_FADE_DURATION, TimeUnit.MILLISECONDS);
        scheduler.shutdown();
        return 0;
    }

    private static final String WORKING_DIR = FileUtils.getHomeDirectory() + "buzzbin/";

    /**
     * Loads the receive screen.
     * Columns: File name, File type, Size, Time Sent
     */
    public void loadReceive() {
        //prevent focus when dialogs in use
        primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                //focused
                if (newPropertyValue && dialogInUse) {
                    if (deleteDialog != null) {
                        deleteDialog.requestFocus();
                    } else if (acceptDialog != null) {
                        acceptDialog.requestFocus();
                    }
            }
        }});

        //setup file and directory choosers
        chooser = new FileChooser();
        chooserDir = new DirectoryChooser();

        //ap gain focus when clicked
        final AnchorPane ap = anchorPane;
        anchorPane.setOnMouseClicked(anchorPane -> ap.requestFocus());
        //make top bar draggable ;)
        rectTop.setOnMousePressed(event -> {
            xOffset = primaryStage.getX() - event.getScreenX();
            yOffset = primaryStage.getY() - event.getScreenY();
        });
        rectTop.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() + xOffset);
            primaryStage.setY(event.getScreenY() + yOffset);
        });

        //set style sheets
        //buttonExit.getStylesheets().add("layouts/button_icons.css");
        //buttonMin.getStylesheets().add("layouts/button_icons.css");
        //set pin if pin has been set
        if (!PIN.equals("")) {
            titleLabel.setText(PIN);
        }
        titleLabel.setCache(true);
        //setup our top buttons
        buttonMin.setFocusTraversable(false);
        buttonExit.setFocusTraversable(false);
        ImageView v1 = new ImageView(LaunchClient.getInstance().images[0]);
        buttonExit.setGraphic(v1);
        buttonExit.setShape(new Circle(1));
        ImageView v2 = new ImageView(LaunchClient.getInstance().images[1]);
        buttonMin.setGraphic(v2);
        buttonMin.setShape(new Circle(1));

        //close on button exit click
        buttonExit.setOnMouseClicked(buttonExit -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable task = () -> {
                System.exit(0);
            };
            scheduler.schedule(task, MainScreen.EXIT_BUTTON_DELAY, TimeUnit.MILLISECONDS);
            scheduler.shutdown();
        });
        //TODO; set iconified
        primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    Runnable r = () -> {
                    };
                    Platform.runLater(r);
                }
            }
        });
        //minimize app on min button click
        buttonMin.setOnMouseClicked(buttonMin -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable task = () -> {
                Runnable r = () -> {
                };
                Platform.runLater(r);
            };
            scheduler.schedule(task, MainScreen.MIN_BUTTON_DELAY, TimeUnit.MILLISECONDS);
            scheduler.shutdown();
        });

        //setup nav button
        buttonNav.setFocusTraversable(false);
        buttonNav.setGraphic(new ImageView(LaunchClient.getInstance().images[4]));
        buttonNav.setOnMouseClicked(event -> {
            if (sendContent == null) loadSend();
            toggleSend();
        });
        anchorContent.getChildren().clear();
        table = new TableView<>();

        double rW = 800 / 4;

        //setup columns
        rColumnName = new TableColumn<>("File Name");
        rColumnName.setPrefWidth(rW);
        rColumnName.setCellValueFactory(new PropertyValueFactory<>("name"));

        rColumnType = new TableColumn<>("File Type");
        rColumnType.setPrefWidth(rW);
        rColumnType.setCellValueFactory(new PropertyValueFactory<>("type"));

        rColumnSize = new TableColumn<>("File Size");
        rColumnSize.setPrefWidth(rW);
        rColumnSize.setCellValueFactory(new PropertyValueFactory<>("size"));

        rColumnTime = new TableColumn<>("Time Received");
        rColumnTime.setPrefWidth(rW);
        rColumnTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        //setup items
        tableItems = FXCollections.observableArrayList();
        //setup listener for FileRequests
        LaunchClient.getInstance().client.setFileRequestListener(new FileRequestListener() {
            @Override
            public void respondRequest(FileSendRequestPacket packet, String fileName) {
                TableItem ti = new TableItem(fileName, FileUtils.getFileExtension(fileName), fileName, "kevin is a furry.");
                ti.uid = packet.fileUID;
                tableItems.add(ti);
            }
            @Override
            public String respondSend(String uid, String fileName) {
                String out = acceptPath + FileUtils.getSeparator() + fileName;
                tableItems.remove(getTableItem(uid));
                return out;
            }
        });
        //table.setFocusTraversable(false);
        table.setItems(tableItems);
        table.getColumns().setAll(rColumnName, rColumnType, rColumnSize, rColumnTime);
        table.setMinWidth(800);
        table.setMinHeight(501);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        //unselect when an empty cell is selected
        table.setRowFactory(tv -> {
            TableRow<TableItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (row.getItem() == null) {
                    table.getSelectionModel().clearSelection();
                }
            });
            return row;
        });
        //setup acc all & del all buttons
        buttonAccAll.setOnMouseClicked(event -> {
            accept(tableItems);
        });
        buttonDelAll.setOnMouseClicked(event -> {
            deleteAllItems();
        });

        //delete selected items on delete
        //TODO: don't hardcode 8 as key code for del
        table.setOnKeyPressed(event -> {
            if (event.getCode().getCode() == 8) {
                deleteSelectedItems();
            } else if (event.getCode().getCode() == KeyCode.ESCAPE.getCode()) {
                table.getSelectionModel().clearSelection();
            } else if (event.getCode().getCode() == KeyCode.ENTER.getCode()) {
                accept(table.getSelectionModel().getSelectedItems());
            }
        });

        FilteredList<TableItem> filteredData = new FilteredList<>(table.getItems(), p -> true);
        textFieldSearch.textProperty().addListener((observable, oldValue, newValue) -> filteredData.setPredicate(myObject -> {
            // If filter is empty/null display all data
            if (newValue == null || newValue.isEmpty()) {
                return true;
            }
            //Otherwise try to match all fields
            String lowerCaseFilter = newValue.toLowerCase();
            boolean b = myObject.hasField(lowerCaseFilter);
            return b;
        }));
        table.setItems(filteredData);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
        });
        anchorContent.getChildren().add(table);
    }

    ObservableList<TableItem> tableItems = null;
    private String pin;

    /**
     * Loads everything associated with the send screen.
     * Setup our QueView.
     */
    public void loadSend() {
        if (sendContent != null) return;
        FXMLLoader loader = null;
        try {
            loader = new FXMLLoader(getClass().getResource("assets/layouts/layout_send.fxml"));
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendContent = (AnchorPane) loader.getNamespace().get("anchorPane");
        anchorContent.getChildren().add(sendContent);
        sendContent.setTranslateY(sendHideY);
        //TOP BAR
        pinField = (TextField) loader.getNamespace().get("pinField");
        buttonSend = (Button) loader.getNamespace().get("buttonSend");
        buttonClear = (Button) loader.getNamespace().get("buttonClear");
        buttonSelect = (Button) loader.getNamespace().get("buttonSelect");
        buttonSelectDir = (Button) loader.getNamespace().get("buttonSelectDir");
        pinLabelResponse = (Label) loader.getNamespace().get("pinLabelResponse");

        buttonClear.setOnMouseClicked(event -> {
            queView.clear();
        });

        //setup pin field
        buttonSend.setDisable(true);
        buttonSend.setOnMouseClicked(event -> {
            String[] paths = queView.getPaths();
            if (paths.length > 0) {
                for (String path: paths) {
                    LaunchClient.getInstance().client.sendFile(path, pin);
                    System.out.println("[" + path + "] -----> " + pin);
                }
            }
            queView.clear();
        });
        pinLabelResponse.setText("NOT READY");
        pinField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                LaunchClient.getInstance().client.sendPinCheckRequest(pinField.getText());
            }
        });
        pinField.setOnKeyPressed(event -> {
            if (event.getCode().getCode() == KeyCode.ENTER.getCode()) {
                anchorPane.requestFocus();
            }
        });
        LaunchClient.getInstance().client.setPinResponseListener((valid, pin, uid) -> {
            pinLabelResponse.setText(valid ? "READY" : "NOT READY");
            pinField.setText(valid ? pin : "");
            buttonSend.setDisable(!valid);
            this.pin = pin;
            if (valid && !uid.equals("")) LaunchClient.getInstance().client.sendHandshake(uid);
        });
        LaunchClient.getInstance().client.setPinUpdateListener(pin -> {
            if (pinField.getText().equals(pin)) {
                pinLabelResponse.setText("NOT READY");
                pinField.setText("");
                buttonSend.setDisable(true);
            }
        });

        pinLabelResponse.setTextFill(Paint.valueOf("#ffffff"));
        rectDragDrop = (Rectangle) loader.getNamespace().get("rectDragDrop");
        rectDragDrop.setCache(true);
        //set style sheets & disable initial focus
        //pinField.getStylesheets().add("layouts/pin_field.css");
        pinField.setCache(true);
        //buttonSend.getStylesheets().add("layouts/button_send.css");
        pinField.setFocusTraversable(false);
        buttonSelect.setFocusTraversable(false);
        buttonSelect.setOnMouseClicked(event -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable task = () -> {
                Runnable r = () -> {
                    List<File> files = chooser.showOpenMultipleDialog(anchorPane.getScene().getWindow());
                    int c = 0;
                    if (files != null) {
                        for (File f : files) {
                            if (f != null) {
                                if (c++ > QueView.MAX_QUED_FILES) break;
                                queView.addItem(f.getAbsolutePath(), f.getName(), false);
                            }
                        }
                    }
                    queView.update(QueView.INITIAL_QUE_DELAY);
                    anchorPane.requestFocus();
                };
                Platform.runLater(r);
            };
            scheduler.schedule(task, MainScreen.SELECT_BUTTON_DELAY, TimeUnit.MILLISECONDS);
            scheduler.shutdown();
        });
        buttonSelectDir.setOnMouseClicked(event -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable task = () -> {
                Runnable r = () -> {
                    File file = chooserDir.showDialog(anchorPane.getScene().getWindow());
                    queView.addItem(file.getAbsolutePath(), file.getName(), false);
                    queView.update(QueView.INITIAL_QUE_DELAY);
                    anchorPane.requestFocus();
                };
                Platform.runLater(r);
            };
            scheduler.schedule(task, MainScreen.SELECT_BUTTON_DELAY, TimeUnit.MILLISECONDS);
            scheduler.shutdown();
        });
        //setup ques
        queView = new QueView(sendContent);
        queView.startX = 10;
        queView.startY = 10;
        queView.setNumItemsPerRow(6);
        //setup drag n drop
        Rectangle dragTarget = rectDragDrop;
        //dragTarget.getChildren().addAll(label,dropped);
        dragTarget.setOnDragOver(event -> {
            if (event.getGestureSource() != dragTarget
                    && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        dragTarget.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                for (File f : db.getFiles()) {
                    queView.addItem(f.getAbsolutePath(), f.getName(), false);
                }
                queView.update(QueView.INITIAL_QUE_DELAY);
            }
            /* let the source know whether the string was successfully
             * transferred and used */
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Booleans to ensure that dialogs are used only once.
     */
    private boolean dialogInUse = false;
    /**
     * Instances of our Accept and Delete com.dialog, respectively.
     */
    private AcceptDialog acceptDialog;
    private DeleteDialog deleteDialog;

    private String acceptPath;

    /**
     * Invoked when items from our table are requested to be accepted.
     * Shows the 'AcceptDialog' com.dialog.
     * Sends a 'FileAcceptPacket' request if com.dialog succeeded.
     * @param items to accept.
     */
    public void accept(TableItem ... items) {
        if (dialogInUse) return;
        if (items.length == 0) return;
        dialogInUse = true;
        acceptDialog = new AcceptDialog(chooserDir, anchorPane.getScene().getWindow());
        acceptDialog.setNumItems(items.length);
        acceptDialog.addFinishListener(SUCCESS -> {
            dialogInUse = false;
            if (SUCCESS) {
                acceptPath = acceptDialog.dir.getAbsolutePath();
                String[] uids = new String[items.length];
                for (int i = 0; i < items.length; i++) uids[i] = items[i].uid;
                LaunchClient.getInstance().client.sendAccept(uids);
            }
            acceptDialog = null;
        });
    }

    /**
     * Invokes 'Accept' from an Observable List.
     * @param items to accept.
     */
    public void accept(ObservableList<TableItem> items) {
        if (items.size() == 0) return;
        TableItem[] it = new TableItem[items.size()];
        int c = 0;
        for (TableItem t: items) {
            it[c++] = t;
        }
        accept(it);
    }

    /**
     * Displays a prompt to delete selected items from the receive table.
     */
    private void deleteSelectedItems() {
        if (dialogInUse) return;
        if (table.getSelectionModel().getSelectedItems().size() == 0 || tableItems.size() == 0) return;
        dialogInUse = true;
        deleteDialog = new DeleteDialog();
        deleteDialog.setNumItems(table.getSelectionModel().getSelectedItems().size());
        deleteDialog.addFinishListener(SUCCESS -> {
            dialogInUse = false;
            if (SUCCESS) {
                tableItems.removeAll(table.getSelectionModel().getSelectedItems());
                table.getSelectionModel().clearSelection();
            }
            deleteDialog = null;
        });
    }

    /**
     * Displays a prompt to delete all items from the receive table.
     */
    private void deleteAllItems() {
        if (dialogInUse) return;
        if (tableItems.size() == 0) return;
        dialogInUse = true;
        DeleteDialog dialog = new DeleteDialog();
        dialog.setNumItems(tableItems.size());
        dialog.addFinishListener(SUCCESS -> {
            dialogInUse = false;
            if (SUCCESS) {
                tableItems.clear();
                table.getSelectionModel().clearSelection();
            }
        });
    }

    /**
     * Gets the index of an item in the table given one of their fields.
     * @param uid uid o 
     * @return index of found item or -1.
     */
    public int getTableItem(String uid) {
        int index = -1;
        for (int i = 0; i < table.getItems().size(); i++) {
            TableItem item = table.getItems().get(i);
            if (item.uid.equals(uid)) return i;
        }
        return index;
    }
}
