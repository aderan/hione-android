package io.agora.board.fast.sample.misc;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.agora.board.fast.model.DocPage;
import java.lang.reflect.Type;
import java.util.HashMap;

public class Utils {

    private static final Gson gson = new Gson();

    public static <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        return gson.fromJson(json, classOfT);
    }

    public static <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
        return gson.fromJson(json, typeOfT);
    }

    private static final HashMap<String, String> docJsonMap = new HashMap<String, String>() {{
        put("8da4cdc71a9845d385a5b58ddfa10b7e",
            "{\"uuid\":\"8da4cdc71a9845d385a5b58ddfa10b7e\",\"type\":\"static\",\"status\":\"Finished\",\"convertedPercentage\":100,\"pageCount\":12,\"images\":{\"1\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/1.png\"},\"2\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/2.png\"},\"3\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/3.png\"},\"4\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/4.png\"},\"5\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/5.png\"},\"6\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/6.png\"},\"7\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/7.png\"},\"8\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/8.png\"},\"9\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/9.png\"},\"10\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/10.png\"},\"11\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/11.png\"},\"12\":{\"width\":1152,\"height\":648,\"url\":\"https://conversion-demo-cn.oss-cn-hangzhou.aliyuncs.com/staticConvert/8da4cdc71a9845d385a5b58ddfa10b7e/12.png\"}}}");
    }};

    public static DocPage[] getDocPages(String taskUuid) {
        StaticDoc doc = Utils.fromJson(docJsonMap.get(taskUuid), StaticDoc.class);
        DocPage[] pages = new DocPage[doc.pageCount];
        for (int i = 0; i < doc.pageCount; i++) {
            String index = String.valueOf(i + 1);
            Image image = doc.images.get(index);
            DocPage page = new DocPage(
                image.url,
                image.width,
                image.height
            );
            pages[i] = page;
        }
        return pages;
    }


}
