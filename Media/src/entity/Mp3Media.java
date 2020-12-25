package entity;

import java.util.ArrayList;
import java.util.List;

public class Mp3Media {
    public static final String IMAGE = "src/fxml/image/";
    public static final String MP3 = "src/fxml/mp3/";
    private String imagePath;
    private String mp3Path;
    private String name;

    public Mp3Media(String image, String mp3){
        this.imagePath = IMAGE + image;
        this.mp3Path = MP3 + mp3;
        this.name = String.valueOf(mp3.toCharArray(),0,mp3.indexOf('.')) + "|";
        //this.name = "||||||";
    }
    public Mp3Media(){
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getName() {
        return name;
    }

    public String getMp3Path() {
        return mp3Path;
    }

    public List getALLMedia(){
        ArrayList list = new ArrayList();
        Mp3Media mp3Media1 = new Mp3Media("bigcityboi.jpg","BigCityBoi.mp3");
        list.add(mp3Media1);
        Mp3Media mp3Media2 = new Mp3Media("danchaua.jpg","DanChauADaDiVoBar.mp3");
        list.add(mp3Media2);
        Mp3Media mp3Media3 = new Mp3Media("nanabumkit.jpg","NaNaBumChit.mp3");
        list.add(mp3Media3);
        return list;
    }
}
