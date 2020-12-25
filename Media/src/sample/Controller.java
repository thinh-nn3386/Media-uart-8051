package sample;

import entity.Mp3Media;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import com.fazecast.jSerialComm.SerialPort;

public class Controller implements Initializable {
    SerialPort sp; // UART
    List<Mp3Media> mp3;
    Media[] media;
    MediaPlayer[] mediaPlayer;
    int currentMp3=0;
    int remotePlay = 0 ; // 0 == pause, 1 == play
    Thread th1;

    @FXML private Button play;
    @FXML private Button pause;
    @FXML private Button next;
    @FXML private Button back;
    @FXML private Button volumeUp;
    @FXML private Button volumeDown;
    @FXML private ImageView imageView;
    @FXML private Text currentTime;
    @FXML private Text time;
    @FXML protected ProgressBar progress;


    /**
     * Tạo một Theard chạy progress bar, Với mỗi một s media chạy, gửi tín hiệu (1) qua uart
     */
    class runProgress implements Runnable{
        @Override
        public void run() {
            double per;
            while (true) {
                if ((per = mediaPlayer[currentMp3].getCurrentTime().toSeconds()/mediaPlayer[currentMp3].getTotalDuration().toSeconds()) !=1 ){
                    //set progress bar
                    progress.setProgress(per);

                    //hiện thị thời gian hiện tại
                    currentTime.setText(String.valueOf((int) mediaPlayer[currentMp3].getCurrentTime().toSeconds()));

                    //mỗi một s, media đang phát nhạc, gửi tín hiệu qua uart
                    if ((int) mediaPlayer[currentMp3].getCurrentTime().toMillis()% 1000 == 0 && remotePlay == 1){

                        try {
                            sp.getOutputStream().write((byte)1);
                            sp.getOutputStream().flush(); Thread.sleep(100);
                        } catch (IOException|InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }

            }

        }
    }

    /**
     * tạo một thread để đọc dữ liệu
     */
    class sendRecv implements Runnable{
        @Override
        public void run() {
            while (true)
            {
                //chờ dữ liệu đến
                while (sp.bytesAvailable() == 0) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //đọc , dữ liệu gửi đến là 2 byte 1 thể hiện nut ng dùng bấm
                byte[] readBuffer = new byte[sp.bytesAvailable()];
                int numRead = sp.readBytes(readBuffer, readBuffer.length);
                System.out.println("Read " + numRead + " bytes." + readBuffer[0]+readBuffer[1]);

                //handle play/pause
                if (readBuffer[0] == 52 && readBuffer[1] == 52){
                    if (remotePlay ==0) {
                        mediaPlayer[currentMp3].play();
                        remotePlay = 1;

                    }
                    else {
                        mediaPlayer[currentMp3].pause();
                        remotePlay = 0;
                    }
                }
                //handle next
                if (readBuffer[0] == 52 && readBuffer[1] == 48 ){
                    remotePlay = 0;
                    next();
                    remotePlay = 1;

                }
                //handle back
                if (readBuffer[0] == 52 && readBuffer[1] == 51){
                    remotePlay = 0;
                    back();
                    remotePlay = 1;
                }
                if (readBuffer[0] == 48 && readBuffer[1] == 57){
                    System.out.println("volume up");
                }
                if (readBuffer[0] == 49 && readBuffer[1] == 53){
                   System.out.println("volume down");
                }
            }
        }
    }

    /**
     * Chạy file mp3, gửi tên bài hát qua cổng uart, tên bài hát + '|' (kí tự báo kết thúc tên)
     */
    private void runMp3() {
        time.setText( "/"+(int)media[currentMp3].getDuration().toSeconds() + " s");
        currentTime.setText("--");
        Image image = new Image(new File(mp3.get(currentMp3).getImagePath()).toURI().toString()) ;
        imageView.setImage(image);
        imageView.setFitWidth(600);
        imageView.setFitHeight(235);

        // progress.setMaxWidth(media.getDuration().toSeconds());
        // display media's metadata
        Map<String,Object> a =  media[currentMp3].getMetadata();
        System.out.println("\t\tTitle: " + a.get("title"));
        System.out.println("\tArtist: " + a.get("artist"));
        System.out.println("\tGenre: " + a.get("genre"));
        //System.out.println(mp3.get(currentMp3).getName());
        for (byte i = 0 ; i < mp3.get(currentMp3).getName().length() ; i ++){
            try {
                sp.getOutputStream().write((byte)mp3.get(currentMp3).getName().charAt(i));
                sp.getOutputStream().flush(); Thread.sleep(25);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("----------------------------------------------------------");
    }
    @FXML
    protected void handlePlay(ActionEvent event) {
        mediaPlayer[currentMp3].play();
        remotePlay = 1;
        progress.progressProperty().unbind();

    }
    @FXML
    protected void handlePause(ActionEvent event){
        mediaPlayer[currentMp3].pause();
        remotePlay = 0;
    }
    @FXML
    protected void handleNext(ActionEvent event){
        next();
    }

    private void next() {
        mediaPlayer[currentMp3].stop();
        currentMp3++;
        if (currentMp3>mp3.size()-1) currentMp3=0;
        runMp3();
        mediaPlayer[currentMp3].play();
    }
    @FXML
    protected void handleBack(ActionEvent event){
        back();
    }
    private void back() {
        mediaPlayer[currentMp3].stop();
        currentMp3--;
        if (currentMp3<0) currentMp3=mp3.size()-1;
        runMp3();
        mediaPlayer[currentMp3].play();
    }

    @FXML
    protected void handleVolumeUp(ActionEvent event){

    }
    @FXML
    protected void handleVolumeDown(ActionEvent event){

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        String comm = null;
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports != null && ports.length>0){
            for (SerialPort port: ports) {
                System.out.println(port.getSystemPortName() + " is connected");
                comm = new String(port.getSystemPortName());
            }
        }
        else {
            System.err.println("No serial port is connected");
        }
        sp = SerialPort.getCommPort(comm);
        sp.setComPortParameters(9600,8,1,0);
        sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING,0,0);
        if (sp.openPort()){
            System.out.println(sp.getSystemPortName() +" is opended");
        }
        else {
            System.out.println(sp.getSystemPortName()+ " is not opended");
            return;
        }

        //bắt đầu nghe dữ liệu gửi đến qua cổng uart
        Thread th2 = new Thread(new sendRecv());
        th2.start();

        mp3 = new Mp3Media().getALLMedia();
        media = new Media[mp3.size()];
        mediaPlayer = new MediaPlayer[mp3.size()];
        for (int i = 0 ; i < mp3.size(); i ++){
            media[i] = new Media(new File(mp3.get(i).getMp3Path()).toURI().toString());
            mediaPlayer[i] = new MediaPlayer(media[i]);
        }
        mediaPlayer[currentMp3].setOnReady(new Runnable() {
            @Override
            public void run() {
                runMp3();
            }
        });

        //đếm s, gửi qua uart
        th1 = new Thread(new runProgress());
        th1.start();
    }


}
