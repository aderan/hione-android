package io.agora.board.fast.sample;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import io.agora.board.fast.sample.cases.hione.HiOneActivity;
import io.agora.board.fast.sample.misc.TestCase;
import io.agora.board.fast.sample.misc.TestCase.RoomInfo;
import java.util.Arrays;
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