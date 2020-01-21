package com.example.mainproject;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class EmbeddingGraph extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "AudioRecordingThread";
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_embedding_graph);

        backButton = findViewById(R.id.back);
        backButton.setOnClickListener(this);

        LineChart chart = (LineChart) findViewById(R.id.chart);
        List<Entry> entires = new ArrayList<Entry>();

        entires.add(new Entry(1,2));
        LineDataSet dataSet = new LineDataSet(entires,"demo entries");
        LineData linedata = new LineData(dataSet);
        chart.setData(linedata);
        chart.invalidate();
    }



    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.back:
                Intent startMain =new Intent(this,MainActivity.class);
                startMain.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(startMain);
                break;
        }
    }
}
