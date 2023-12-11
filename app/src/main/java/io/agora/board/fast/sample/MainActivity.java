package io.agora.board.fast.sample;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.herewhite.sdk.internal.Logger;
import io.agora.board.fast.sample.cases.hione.HiOneActivity;
import io.agora.board.fast.sample.misc.TestCase;
import io.agora.board.fast.sample.misc.TestCase.RoomInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView testcaseRv;

    private TestCaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupUi();

        new Thread(
            () -> testReadAssets()
        ).start();
    }

    private void testReadAssets() {
        long startTime = SystemClock.elapsedRealtime();
        readLargeFileFromAssets("random_file_100m.bin");
        long endTime = SystemClock.elapsedRealtime();
        Logger.info("time cost read assets 100m " + (endTime - startTime));
    }

    private void readLargeFileFromAssets(String fileName) {
        AssetManager assetManager = getAssets();

        try {
            InputStream inputStream = assetManager.open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {

            }

            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupUi() {
        testcaseRv = findViewById(R.id.testcase_rv);
        adapter = new TestCaseAdapter(getTestCases());
        adapter.setOnItemClickListener(this::startTestCase);
        testcaseRv.setAdapter(adapter);
    }

    public List<TestCase> getTestCases() {
        return Collections.singletonList(
            new TestCase(
                "HiOne Sample",
                "Extension Sample 1",
                HiOneActivity.class,
                new RoomInfo(Constants.SAMPLE_ROOM_UUID, Constants.SAMPLE_ROOM_TOKEN, true)
            )
        );
    }

    private void startTestCase(TestCase testCase) {
        if (testCase.isLive()) {
            Intent intent = new Intent(this, testCase.clazz);
            intent.putExtra(Constants.KEY_ROOM_UUID, testCase.roomInfo.roomUUID);
            intent.putExtra(Constants.KEY_ROOM_TOKEN, testCase.roomInfo.roomToken);
            startActivity(intent);
        }
    }
}