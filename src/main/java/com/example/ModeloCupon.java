package com.example;

public class ModeloCupon {
    private String bono;
    private String date;

    public ModeloCupon(String bono, String date) {
        this.bono = bono;
        this.date = date;
    }

    public String getBono() {
        return bono;
    }

    public void setBonoAndDate(String bono,String date) {
        this.bono = bono;
        this.date = date;
    }

    public String getDate() {
        return date;
    }

}
