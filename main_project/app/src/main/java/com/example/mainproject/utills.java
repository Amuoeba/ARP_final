package com.example.mainproject;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

public class utills  <T extends Comparable<T>>{

    public static short min( short[] array){
        short currentMin;
        currentMin = array[0];
        for(int i = 0; i<array.length;i++){
            if(array[i] < currentMin){
                currentMin = array[i];
            }
        }
        return currentMin;
    }

    public static short max( short[] array){
        short currmax;
        currmax = array[0];
        for(int i = 0; i>array.length;i++){
            if(array[i] > currmax){
                currmax = array[i];
            }
        }
        return currmax;
    }

    public static float min( float[] array){
        float currentMin;
        currentMin = array[0];
        for(int i = 0; i<array.length;i++){
            if(array[i] < currentMin){
                currentMin = array[i];
            }
        }
        return currentMin;

    }

    public static float max( float[] array){
        float currmax;
        currmax = array[0];
        for(int i = 0; i>array.length;i++){
            if(array[i] > currmax){
                currmax = array[i];
            }
        }
        return currmax;
    }

    public static float average(float[] array){
        float summ = 0;

        for(int i = 0; i < array.length;i++){
            summ += array[i];
        }
        return summ/array.length;
    }


    public static float[][] transposeArray(float[][] array){
        if (array.length < 1) {
            return array;
        }

        // Table isn't empty
        int nRows = array.length;
        int nCols = array[0].length;
        float[][] transpose = new float[nCols][nRows];

        // Do the transpose
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                transpose[j][i] = array[i][j];
            }
        }

        return transpose;
    }

    public static double[][] transposeArray(double[][] array){
        if (array.length < 1) {
            return array;
        }

        // Table isn't empty
        int nRows = array.length;
        int nCols = array[0].length;
        double[][] transpose = new double[nCols][nRows];

        // Do the transpose
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                transpose[j][i] = array[i][j];
            }
        }

        return transpose;
    }


}




