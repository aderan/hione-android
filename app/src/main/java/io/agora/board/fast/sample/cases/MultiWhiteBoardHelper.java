package io.agora.board.fast.sample.cases;

import android.text.TextUtils;
import android.util.Log;

import com.herewhite.sdk.WhiteboardView;
import com.herewhite.sdk.domain.Promise;
import com.herewhite.sdk.domain.SDKError;
import com.herewhite.sdk.domain.Scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.agora.board.fast.FastRoom;
import io.agora.board.fast.sample.R;

/**
 * 白板多窗口辅助类
 */
public class MultiWhiteBoardHelper {

    private static final String TAG = "WhiteBoardHelper";
    private static final String DIVIDE_BOARD = "|";
    private static final String DIVIDE_BOARD_REGEX = "\\|";
    private final FastRoom fastRoom;
    private final List<BoardListItem> boardList = Collections.synchronizedList(new ArrayList<>());


    public MultiWhiteBoardHelper(FastRoom fastRoom) {
        this.fastRoom = fastRoom;
    }

    /**
     * 设置白板列表，可用于切换
     * @param list 白板列表
     */
    public void setWhiteBoardList(List<BoardListItem> list){
        boardList.clear();
        boardList.addAll(list);
    }

    /**
     * 获取白板列表，可用于数据同步
     * @return 白板列表
     */
    public List<BoardListItem> getWhiteBoardList() {
        ArrayList<BoardListItem> ret = new ArrayList<>();
        synchronized (boardList) {
            for (BoardListItem i : boardList) {
                BoardListItem item = new BoardListItem();
                item.id = i.id;
                item.name = i.name;
                item.status = i.status;
                item.scale = i.scale;
                item.totalPage = i.totalPage;
                item.activityPage = i.activityPage;
                item.type = i.type;
                ret.add(item);
            }
        }
        return ret;
    }


    /**
     * 添加白板，用于插入空白白板、图片、ppt
     * @param path 白板路径，需要是唯一且不带"/","|"特殊符号，如果是图片或者ppt，请带类型后缀，如.png，否则白板列表将无法识别类型
     * @param scenes 白板上显示的场景列表
     */
    public void addWhiteBoard(String path, Scene[] scenes) {
        synchronized (boardList) {
            for (BoardListItem item : boardList) {
                if (item.id.equals(path)) {
                    Log.w(TAG, "addWhiteBoard >> The board existed! path=" + path);
                    return;
                }
            }
        }

        BoardListItem boardListItem = new BoardListItem();
        boardListItem.id = path;
        boardListItem.name = path;
        boardListItem.totalPage = scenes.length;
        boardListItem.type = getBoardItemType(path);
        boardList.add(boardListItem);

        Scene[] newScenes = new Scene[scenes.length];
        for (int i = 0; i < scenes.length; i++) {
            Scene scene = scenes[i];
            newScenes[i] = new Scene(
                    path + DIVIDE_BOARD + scene.getName(),
                    scene.getPpt()
            );
        }
        fastRoom.getRoom().putScenes(
                "/",
                newScenes,
                Integer.MAX_VALUE
        );
    }

    /**
     * 切换白板，白板路径或者页数不存在时会切换失败
     * @param path 白板路径
     * @param page 白板页数
     * @param complete 执行结束回调
     */
    public void switchWhiteBoard(String path, int page, Runnable complete) {
        fastRoom.getRoom().getEntireScenes(new Promise<Map<String, Scene[]>>() {
            @Override
            public void then(Map<String, Scene[]> stringMap) {
                Scene[] scenes = stringMap.get("/");
                switchWhiteBoardInner(scenes, path, page);
                if (complete != null) {
                    complete.run();
                }
            }

            @Override
            public void catchEx(SDKError t) {
                Log.e(TAG, "switchWhiteBoard catchEx >> SDKError = " + t);
            }
        });
    }

    /**
     * 关闭白板
     * @param path 白板路径
     * @param complete 执行结束回调
     */
    public void destroyWhiteBoard(String path, Runnable complete) {
        fastRoom.getRoom().getEntireScenes(new Promise<Map<String, Scene[]>>() {
            @Override
            public void then(Map<String, Scene[]> stringMap) {
                Scene[] scenes = stringMap.get("/");
                List<Scene> removeScenes = new ArrayList<>();
                if (scenes != null) {
                    for (Scene scene : scenes) {
                        if (scene.getName().split(DIVIDE_BOARD_REGEX)[0].equals(path)) {
                            removeScenes.add(scene);
                        }
                    }
                }
                String nextBoardPath = "";
                int nextActivityPage = 0;
                boolean nextFlag = false;

                synchronized (boardList) {
                    Iterator<BoardListItem> iterator = boardList.iterator();
                    while (iterator.hasNext()) {
                        BoardListItem item = iterator.next();
                        if (item.id.equals(path)) {
                            nextFlag = true;
                            iterator.remove();
                        } else if (nextFlag) {
                            nextBoardPath = item.id;
                            nextActivityPage = item.activityPage;
                            break;
                        }
                    }
                }

                if (TextUtils.isEmpty(nextBoardPath) && boardList.size() > 0) {
                    BoardListItem boardListItem = boardList.get(0);
                    nextBoardPath = boardListItem.id;
                    nextActivityPage = boardListItem.activityPage;
                }

                switchWhiteBoardInner(scenes, nextBoardPath, nextActivityPage);

                for (Scene scene : removeScenes) {
                    fastRoom.getRoom().removeScenes("/" + scene.getName());
                }
                if(complete != null){
                    complete.run();
                }
            }

            @Override
            public void catchEx(SDKError t) {
                Log.e(TAG, "destroyWhiteBoard catchEx >> SDKError = " + t);
            }
        });
    }

    private void switchWhiteBoardInner(Scene[] scenes, String path, int page) {
        int pathIndex = -1;
        int sceneIndex = -1;
        if (scenes != null) {
            for (int i = 0; i < scenes.length; i++) {
                if (scenes[i].getName().split(DIVIDE_BOARD_REGEX)[0].equals(path)) {
                    if (pathIndex < 0) {
                        pathIndex = i;
                    }
                    sceneIndex++;
                    if (sceneIndex == page) {
                        break;
                    }
                }
            }
        }
        sceneIndex += pathIndex;
        if (sceneIndex < 0 || sceneIndex >= scenes.length) {
            Log.e(TAG, "switchWhiteBoard then >> Can not find the scene index. path=" + path + ", page=" + page);
            return;
        }

        for (BoardListItem item : boardList) {
            if (item.id.equals(path)) {
                item.activityPage = page;
                item.status = BoardItemStatus.active;
            } else {
                item.status = BoardItemStatus.inactive;
            }
        }

        String script = "window.manager.setMainViewSceneIndex(" + sceneIndex + ")";
        Log.d(TAG, "switchWhiteBoard >> script=" + script);
        WhiteboardView whiteboardView = fastRoom.getFastboardView().findViewById(R.id.fast_whiteboard_view);
        whiteboardView.evaluateJavascript(script);
    }

    private BoardItemType getBoardItemType(String name) {
        String[] split = name.split("\\.");
        if (split.length > 1) {
            return BoardItemType.valueOf(split[1]);
        }
        return BoardItemType.whiteboard;
    }

    /**
     * 白板状态
     */
    public enum BoardItemStatus {
        // 选中状态
        active,
        // 非选中状态
        inactive
    }

    /**
     * 白板类型
     */
    public enum BoardItemType {
        whiteboard,
        ppt,
        pptx,
        doc,
        docx,
        pdf,
        png,
        jpg,
        gif
    }

    /**
     * 白板信息
     */
    public static class BoardListItem {
        // 唯一标识，当前使用的是path值
        public String id;
        // 可以不唯一，外部可自行修改
        public String name;
        // 白板状态
        public BoardItemStatus status = BoardItemStatus.inactive;
        // 白板缩放
        public float scale = 1.0f;
        // 白板总页数
        public int totalPage = 0;
        // 白板选中页
        public int activityPage = 0;
        // 白板类型
        public BoardItemType type = BoardItemType.whiteboard;
    }

}
