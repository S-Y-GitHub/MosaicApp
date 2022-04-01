import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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

        TextField tfColumns=new TextField();
        tfColumns.setTextFormatter(getTextFormatter());
        tfColumns.setOnKeyReleased(e->{
            String text=tfColumns.getText();
            if(!text.equals("")){
                generator.setColumns(Integer.parseInt(text));
            }
        });
        Label labelMatrixX=new Label("x");
        TextField tfRows=new TextField();
        tfRows.setTextFormatter(getTextFormatter());
        tfRows.setOnKeyReleased(e->{
            String text=tfRows.getText();
            if(!text.equals("")){
                generator.setRows(Integer.parseInt(text));
            }
        });
        HBox hbMatrixSize=new HBox(tfColumns,labelMatrixX,tfRows);

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
            File result=new File(labelResultPath.getText());
            try{
                generator.generate(original,result);
            }catch(Exception ee){
                errorAlert("failed to generate");
            }
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

    private TextFormatter<String> getTextFormatter(){
        Pattern notNumber=Pattern.compile("[^0-9]+");
        TextFormatter<String> formatter=new TextFormatter<>(c->{
            String text=c.getText();
            String newText=notNumber.matcher(text).replaceAll("");
            int diff=text.length()-newText.length();
            c.setAnchor(c.getAnchor()-diff);
            c.setCaretPosition(c.getCaretPosition()-diff);
            c.setText(newText);
            return c;
        });
        return formatter;
    }

    private void errorAlert(String message){
        Alert alert=new Alert(AlertType.ERROR,message,ButtonType.CLOSE);
        alert.showAndWait();
    }

    public static void main(String[] args){
        launch(args);
    }
}
