package org.worldbank.transport.driver;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.worldbank.transport.driver.models.AccidentDetails;
import org.worldbank.transport.driver.models.DriverSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, loadRecord(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    public String loadRecord() {
        try {

            BufferedReader ir = new BufferedReader(new InputStreamReader(getAssets()
                    .open("json/data/DriverRecord.json"), "UTF-8"));

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = ir.readLine()) != null) {
                stringBuilder.append(line);
            }
            ir.close();
            String responseStr = stringBuilder.toString();

            Gson gson = new GsonBuilder().create();
            DriverSchema record = gson.fromJson(responseStr, DriverSchema.class);

            Log.d("MainActivity:loadRecord", responseStr);
            final AccidentDetails deets = record.AccidentDetails;
            return deets.Severity.name();

        } catch (IOException e) {
            e.printStackTrace();
            return "Something broke.";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
