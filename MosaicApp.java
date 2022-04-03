import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

public class MosaicApp extends Application{
    MosaicGenerator generator=new MosaicGenerator();

    List<File> assetFiles=new ArrayList<>();

    final private int INSET=10;
    @Override
    public void start(Stage stage){
        Label labelAtom=new Label("atoms");

        ListView<String> listAssets=new ListView<>();
        listAssets.setOnDragOver(e->{
            Dragboard board=e.getDragboard();
            if(board.hasFiles()){
                e.acceptTransferModes(TransferMode.MOVE);
            }
        });
        listAssets.setOnDragDropped(e->{
            Dragboard board=e.getDragboard();
            if(board.hasFiles()){
                board.getFiles().forEach(f->{
                    String name=f.getName();
                    if(isReadableFileFormat(name)&&!assetFiles.contains(f)){
                        listAssets.getItems().add(name);
                        assetFiles.add(f);
                        try{
                            generator.addImage(f);
                        }catch(IOException ee){
                            errorAlert("failed to read file");
                        }
                    }
                });
                e.setDropCompleted(true);
            }else{
                e.setDropCompleted(false);
            }
        });

        ImageView ivThumbnail=new ImageView();
        ivThumbnail.setPreserveRatio(true);
        listAssets.setOnMouseClicked(e->{
            int index=listAssets.getSelectionModel().getSelectedIndex();
            if(index==-1){
                return;
            }
            File imageFile=assetFiles.get(index);
            
            Image image=new Image(imageFile.getPath());
            ivThumbnail.setImage(image);
        });

        Pane paneThumbnail=new Pane(ivThumbnail);
        ivThumbnail.fitWidthProperty().bind(paneThumbnail.widthProperty());
        ivThumbnail.fitHeightProperty().bind(paneThumbnail.heightProperty());

        BorderPane bpAtoms=new BorderPane();
        bpAtoms.setLeft(labelAtom);
        bpAtoms.setRight(paneThumbnail);

        Button btnRemove=new Button("remove");
        btnRemove.setMaxWidth(Double.MAX_VALUE);
        btnRemove.setOnAction(e->{
            int index=listAssets.getSelectionModel().getSelectedIndex();
            if(index==-1){
                return;
            }
            try{
                generator.removeImage(assetFiles.get(index));
            }catch(IOException ee){
                errorAlert("fetal error has occurred\nplease reboot this app");
            }
            assetFiles.remove(index);
            listAssets.getItems().remove(index);
            ivThumbnail.setImage(null);
        });

        BorderPane bpAssets=new BorderPane();
        bpAssets.setTop(bpAtoms);
        bpAssets.setCenter(listAssets);
        bpAssets.setBottom(btnRemove);
        bpAssets.setPadding(new Insets(INSET));

        Label labelMatrixSize=new Label("matrix size");

        TextField tfColumns=new TextField(String.valueOf(generator.getColumns()));
        tfColumns.setTextFormatter(getTextFormatter(Format.INT));
        tfColumns.setOnKeyReleased(e->{
            String text=tfColumns.getText();
            if(!text.equals("")){
                generator.setColumns(Integer.parseInt(text));
            }
        });
        tfColumns.focusedProperty().addListener((arg,oldV,newV)->tfColumns.setText(String.valueOf(generator.getColumns())));
        Label labelMatrixX=new Label("x");
        TextField tfRows=new TextField(String.valueOf(generator.getRows()));
        tfRows.setTextFormatter(getTextFormatter(Format.INT));
        tfRows.setOnKeyReleased(e->{
            String text=tfRows.getText();
            if(!text.equals("")){
                generator.setRows(Integer.parseInt(text));
            }
        });
        tfRows.focusedProperty().addListener((arg,oldV,newV)->tfRows.setText(String.valueOf(generator.getRows())));
        HBox hbMatrixSize=new HBox(tfColumns,labelMatrixX,tfRows);

        Label labelScale=new Label("scale");

        Label labelScaleX=new Label("x");
        TextField tfScale=new TextField(String.valueOf(generator.getScale()));
        tfScale.setTextFormatter(getTextFormatter(Format.DOUBLE));
        tfScale.setOnKeyReleased(e->{
            String text=tfScale.getText();
            text=text.replaceAll("\\.+",".");
            if(text.startsWith(".")){
                text=0+text;
            }
            if(!text.equals("")){
                int index=text.lastIndexOf(".");
                if(text.indexOf(".")!=index){
                    text=text.substring(0,index);
                }
                String[] split=text.split("\\.");
                if(split.length>2){
                    text=split[0]+"."+split[1];
                }
                generator.setScale(Double.parseDouble(text));
                tfScale.setText(text);
                tfScale.positionCaret(text.length());
            }
        });
        tfScale.focusedProperty().addListener((arg,oldV,newV)->tfScale.setText(String.valueOf(generator.getScale())));
        HBox hbScale=new HBox(labelScaleX,tfScale);

        CheckBox cbAlpha=new CheckBox("enable alpha");
        cbAlpha.setOnAction(e->{
            generator.setHasAlpha(cbAlpha.isSelected());
        });
        CheckBox cbKeepAspect=new CheckBox("keep aspect ratio");
        cbKeepAspect.setOnAction(e->{
            generator.setKeepAspect(cbKeepAspect.isSelected());
        });

        VBox vbSettings=new VBox(
            labelMatrixSize,hbMatrixSize,new Separator(),
            labelScale,hbScale,new Separator(),
            cbAlpha,cbKeepAspect
        );
        vbSettings.setPadding(new Insets(INSET));


        Label labelOriginal=new Label("original");

        File picturesDirectory=new File(System.getProperty("user.home")+"/Pictures");

        Button btnSelectOriginal=new Button("select");
        Label labelOriginalPath=new Label();
        ExtensionFilter exReadableImages=getReadableFileFormatFilter();
        btnSelectOriginal.setOnAction(e->{
            FileChooser chooser=new FileChooser();
            chooser.setTitle("select original file");
            if(picturesDirectory.exists()){
                chooser.setInitialDirectory(picturesDirectory);
            }
            chooser.getExtensionFilters().add(exReadableImages);
            File original=chooser.showOpenDialog(stage);
            if(original!=null){
                labelOriginalPath.setText(original.getAbsolutePath());
            }
        });

        HBox hbSelectOriginal=new HBox(btnSelectOriginal,labelOriginalPath);

        Label labelResult=new Label("result");
        Button btnSelectResult=new Button("select");
        Label labelResultPath=new Label();
        ExtensionFilter exWritableImages=getWritableFileFormatFilter();
        btnSelectResult.setOnAction(e->{
            FileChooser chooser=new FileChooser();
            chooser.setTitle("select result file");
            if(picturesDirectory.exists()){
                chooser.setInitialDirectory(picturesDirectory);
            }
            chooser.getExtensionFilters().add(exWritableImages);
            chooser.setInitialFileName("mosic.jpeg");
            File result=chooser.showSaveDialog(stage);
            if(result!=null){
                labelResultPath.setText(result.getAbsolutePath());
            }
        });

        HBox hbSelectResult=new HBox(btnSelectResult,labelResultPath);

        Button btnGenerate=new Button("generate");
        btnGenerate.setOnAction(e->{
            File original=new File(labelOriginalPath.getText());
            if(!original.exists()){
                Toolkit.getDefaultToolkit().beep();
                errorAlert("original file is not selected or not exists");
                return;
            }
            final MosaicGenerator GENERATOR=generator;
            generator=new MosaicGenerator(generator.assets,generator.hasAlpha,generator.keepAspect,generator.columns,generator.rows,generator.scale);

            File result=new File(labelResultPath.getText());

            Stage stProgress=new Stage();
            Task<Void> task=new Task<Void>() {
                @Override
                public Void call(){
                    try{
                        GENERATOR.generate(original,result);
                        while(GENERATOR.progress<100){
                            updateProgress(GENERATOR.progress,100);
                            if(GENERATOR.progress==-1){
                                throw new Exception();
                            }
                        }
                    }catch(Exception ee){
                        Platform.runLater(()->errorAlert("failed to generate"));
                    }
                    Platform.runLater(()->stProgress.close());
                    return null;
                }
            };
            Thread thread=new Thread(task);
            thread.setDaemon(true);

            Label labelFiles=new Label(original.getPath()+" -> "+result.getPath());
            
            ProgressBar progressBar=new ProgressBar();
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.progressProperty().bind(task.progressProperty());

            VBox vb=new VBox(labelFiles,progressBar);
            vb.setAlignment(Pos.CENTER);

            stProgress.setTitle("progress");
            stProgress.setScene(new Scene(vb));
            stProgress.setWidth(500);
            stProgress.setHeight(200);
            stProgress.setResizable(false);
            stProgress.show();
            stProgress.setOnCloseRequest(ee->ee.consume());
            thread.start();
        });
        btnGenerate.setMaxWidth(Double.MAX_VALUE);

        VBox vbMain=new VBox(
            labelOriginal,hbSelectOriginal,new Separator(),
            labelResult,hbSelectResult,new Separator(),
            btnGenerate
        );
        vbMain.setPadding(new Insets(INSET));


        BorderPane bpRoot=new BorderPane();
        bpRoot.setLeft(bpAssets);
        bpRoot.setCenter(vbMain);
        bpRoot.setRight(vbSettings);
        bpRoot.setPadding(new Insets(INSET));

        Scene scene=new Scene(bpRoot);

        stage.setTitle("MosaicApp");
        stage.setScene(scene);
        stage.setWidth(1280);
        stage.setHeight(720);
        stage.show();
    }

    final String[] READABLE_FILE_FORMAT=ImageIO.getReaderFileSuffixes();
    final String[] WRITABLE_FILE_FORMAT=ImageIO.getWriterFileSuffixes();

    private boolean isReadableFileFormat(String fileName){
        for(String readableFormat:READABLE_FILE_FORMAT){
            if(fileName.endsWith(readableFormat)){
                return true;
            }
        }
        return false;
    }

    private ExtensionFilter getReadableFileFormatFilter(){
        List<String> readableFileExtensions=new ArrayList<>();
        for(String readableFileFormat:READABLE_FILE_FORMAT){
            readableFileExtensions.add("*."+readableFileFormat);
        }
        return new ExtensionFilter("image",readableFileExtensions);
    }

    private ExtensionFilter getWritableFileFormatFilter(){
        List<String> writableFileExtensions=new ArrayList<>();
        for(String writableFileFormat:WRITABLE_FILE_FORMAT){
            writableFileExtensions.add("*."+writableFileFormat);
        }
        return new ExtensionFilter("image",writableFileExtensions);
    }

    private TextFormatter<?> getTextFormatter(Format f){
        TextFormatter<String> formatter=new TextFormatter<>(c->{
            String text=c.getText();
            String newText="";
            switch(f){
                case INT:{
                    newText=text.replaceAll("[^0-9]+","");
                    break;
                }
                case DOUBLE:{
                    newText=text.replaceAll("[^0-9\\.]+","");
                    break;
                }
            }
            int diff=text.length()-newText.length();
            c.setAnchor(c.getAnchor()-diff);
            c.setCaretPosition(c.getCaretPosition()-diff);
            c.setText(newText);
            return c;
        });
        return formatter;
    }

    private static enum Format{
        INT,
        DOUBLE
    }

    private void errorAlert(String message){
        Alert alert=new Alert(AlertType.ERROR,message,ButtonType.CLOSE);
        alert.showAndWait();
    }

    public static void main(String[] args){
        launch(args);
    }
}
