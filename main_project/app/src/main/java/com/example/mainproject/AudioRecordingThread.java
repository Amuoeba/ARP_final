package com.example.mainproject;


import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;


import android.media.AudioFormat;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.mainproject.MFCC.MFCC;
import com.konovalov.vad.VadConfig;
import com.konovalov.vad.Vad;
import com.konovalov.vad.VadListener;

//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.pytorch.Module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;


//import Jama.Matrix;

//import org.nd4j.linalg.dimensionalityreduction.PCA;



public class AudioRecordingThread {

    private static final String TAG = "AudioRecordingThread";
    private Thread thread;
    private Thread embedderThread;

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 2000;
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    short[] recordingBuffer = new short[RECORDING_LENGTH];
    int recordingOffset = 0;

    private boolean isVoiceDetected = false;
    private boolean shouldRunEmbedder = false;

    private AudioRecord audioRecord;
    private int bufferSize;
    private Listener callback;
    private Vad vad;
    private final ReentrantLock recordingBufferLock = new ReentrantLock();
    Module embedderModule = null;

    public AudioRecordingThread(Listener callback, VadConfig config, Module embedderModule){
        this.callback = callback;
        this.vad = new Vad(config);
        this.embedderModule = embedderModule;
    }


    public synchronized void start(){

        audioRecord = createAudioRecord();
        audioRecord.startRecording();
        thread = new Thread(new ProcessAudio());
        embedderThread = new Thread(new Embedder());
//        System.out.println("Debug: thread started");
        Log.d(TAG,"Debug: thread started");
        thread.start();
//        System.out.println("Debug: Vad started");
        Log.d(TAG,"Debug: Vad started");
        vad.start();
        Log.d(TAG,"Starting embedder thread");
        embedderThread.start();
    }

    public synchronized void startEmbedder(){
        embedderThread = new Thread(new Embedder());
        Log.d(TAG,"Starting embedder thread");
        embedderThread.start();
    }

    private AudioRecord createAudioRecord(){
        int audioSource = MediaRecorder.AudioSource.MIC;
        int samplingRate = SAMPLE_RATE;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioRecord.getMinBufferSize(samplingRate,channelConfig,audioFormat);
        this.bufferSize = bufferSize;
        final AudioRecord audioRecord =new AudioRecord(audioSource,samplingRate,channelConfig,audioFormat,bufferSize);

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            System.out.println("Debug: Retruning audio recoreder");
            return audioRecord;
        }else{
            audioRecord.release();
        }
        return null;
    }

    private class ProcessAudio implements Runnable{

        @Override
        public void run(){
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            short[] buffer = new short[bufferSize/8];
            float[] floatBuffer = new float[bufferSize/8];


            while (true){
                int numberRead = audioRecord.read(buffer,0,buffer.length);
                int maxLength = recordingBuffer.length;
                int newRecordingOffset = recordingOffset + numberRead;
                int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
                int firstCopyLength = numberRead - secondCopyLength;



                for(int i= 0; i<buffer.length;i++){
                    floatBuffer[i] = (float)buffer[i]/32767;
//                    Log.d(TAG,"Float: " + floatBuffer[i]);
                }
//                Log.d(TAG,"Buffer size: " + bufferSize/8);
//                Log.d(TAG,"Short Min: "+utills.min(buffer) + " Short Max: " + utills.max(buffer)
//                        + "FloatMin: "+utills.min(floatBuffer) + " Float Max: " + utills.max(floatBuffer)
//                        +" Float average: " + utills.average(floatBuffer));
                isSpeechDetected(buffer);

                if(isVoiceDetected){
//                    Log.d(TAG,"Writing to buffer");
//                    Log.d(TAG,"Recorder buffer:" + Arrays.toString(recordingBuffer));
                    recordingBufferLock.lock();
                    try {
                        System.arraycopy(buffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
                        System.arraycopy(buffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
                        recordingOffset = newRecordingOffset % maxLength;
                    } finally {
                        recordingBufferLock.unlock();
                    }
                }
            }
        }


        private void isSpeechDetected(short[] buffer){
            vad.isContinuousSpeech(buffer, new VadListener() {
                @Override
                public void onSpeechDetected() {

                    callback.onSpeechDetected();
                    isVoiceDetected = true;
                    shouldRunEmbedder = true;
                }

                @Override
                public void onNoiseDetected() {

                    callback.onNoiseDetected();
                    isVoiceDetected = false;
                }
            });
        }
    }


    private class Embedder implements Runnable{

        short[] inputBuffer = new short[RECORDING_LENGTH];
        double[] floatInputBuffer = new double[RECORDING_LENGTH];
        int[] sampleRateList = new int[] {SAMPLE_RATE};


        @Override
        public void run() {
            while(true){
                if(isVoiceDetected){
                    recordingBufferLock.lock();
                    try {
                        int maxLength = recordingBuffer.length;
                        int firstCopyLength = maxLength - recordingOffset;
                        int secondCopyLength = recordingOffset;
                        System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
                        System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
                    } finally {
                        recordingBufferLock.unlock();
                    }
                    Log.d(TAG,"Running embbeder");
//                    Log.d(TAG,"Embedder input"+Arrays.toString(inputBuffer));
//                    Log.d(TAG,"Lenght of inoput buffer: " + inputBuffer.length);

                    for(int i= 0; i<inputBuffer.length;i++) {
                        floatInputBuffer[i] = (double) inputBuffer[i] / 32767;
                    }
                    Log.d(TAG,Arrays.toString(inputBuffer));
                    Log.d(TAG,Arrays.toString(floatInputBuffer));
                    MFCC mfccConvert = new MFCC();
//                    float[] mfccResults = mfccConvert.process(floatInputBuffer);
//                    float[][] mfccResults = mfccConvert.process_2d(floatInputBuffer);
                    double[][] results = utills.transposeArray(mfccConvert.dctMfcc(floatInputBuffer));
                    Log.d(TAG,"Mfcc rows: "+results.length + " Mfcc columns: " + results[0].length);
//                    Log.d(TAG,"Mfcc rows: "+mfccResults.length + " Mfcc columns: " + mfccResults[0].length);


                    final long[] shape = new long[]{2, 160, 40};
                    final float[] embedInputTensor = new float[12800];
                    int offset = 0;
                    for(int i = 0;i < 160;i++){
                        for(int j = 0; j<40;j++){
                            embedInputTensor[(i*40)+j] = (float)results[i][j];
                            offset += 1;
                        }
                    }
                    Log.d(TAG,"Offset: " + offset);
                    for(int i = 0;i < 160;i++){
                        for(int j = 0; j<40;j++){
                            embedInputTensor[offset+(i*40)+j] = (float)results[i][j];
                        }
                    }



                    Tensor newTesnor = Tensor.fromBlob(embedInputTensor,shape);
                    Log.d(TAG,"Out tensor: " + newTesnor);

                    final Tensor out = embedderModule.forward(IValue.from(newTesnor)).toTensor();
                    Log.d(TAG,"Out tensor shape: " + Arrays.toString(out.shape()));//Arrays.toString(out.getDataAsFloatArray()));

                    float[][] embeddings = new float[2][256];
                    for(int i= 0; i<out.shape()[0];i++){
                        for(int j= 0; j<out.shape()[1];j++){
                            embeddings[i][j] = (float)out.getDataAsFloatArray()[i*256+j];
                        }
                    }

                    Log.d(TAG,"Pca array shape: " + TrainedPCA.pcaArray.length + " | " + TrainedPCA.pcaArray[0].length);

//                    PCA pca = new PCA(new Matrix(embeddings));
//                    Matrix transformedEmbeddings  = pca.transform(new Matrix(embeddings),PCA.TransformationType.WHITENING);
//
//                    int rows = transformedEmbeddings.getRowDimension();
//                    int cols = transformedEmbeddings.getColumnDimension();
//                    Log.d(TAG,"Rows: " + rows + " Cols: " +cols + transformedEmbeddings.toS);

//                    List<INDArray> embeddingArrays = new ArrayList<INDArray>();
//                    for(int i = 0; i < embeddings.length;i++){
//                        embeddingArrays.add(new NDArray(embeddings[i]));
//                    }
//                    INDArray embeddingMatrix = new NDArray(embeddingArrays, new int []{2,256});
//                    INDArray pcaReduced = PCA.pca_factor(embeddingMatrix,2,true);
//                    Log.d(TAG,"pca reduced: " + pcaReduced.size(0));



                    shouldRunEmbedder = false;
                }
            }
        }


    }






    public interface Listener {
        void onSpeechDetected();

        void onNoiseDetected();
    }

}
