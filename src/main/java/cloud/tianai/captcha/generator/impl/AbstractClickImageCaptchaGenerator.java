package cloud.tianai.captcha.generator.impl;

import cloud.tianai.captcha.generator.AbstractImageCaptchaGenerator;
import cloud.tianai.captcha.generator.common.model.dto.ClickImageCheckDefinition;
import cloud.tianai.captcha.generator.common.model.dto.GenerateParam;
import cloud.tianai.captcha.generator.common.model.dto.ImageCaptchaInfo;
import cloud.tianai.captcha.generator.common.util.CaptchaImageUtils;
import cloud.tianai.captcha.resource.ImageCaptchaResourceManager;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import lombok.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Author: 天爱有情
 * @date 2022/4/27 11:46
 * @Description 点选验证码 点选验证码分为点选文字和点选图标等
 */
public abstract class AbstractClickImageCaptchaGenerator extends AbstractImageCaptchaGenerator {

    /** 参与校验的数量. */
    @Getter
    @Setter
    protected Integer checkClickCount = 4;
    /** 干扰数量. */
    @Getter
    @Setter
    protected Integer interferenceCount = 2;


    public AbstractClickImageCaptchaGenerator(ImageCaptchaResourceManager imageCaptchaResourceManager, boolean initDefaultResource) {
        super(imageCaptchaResourceManager, initDefaultResource);
    }

    public AbstractClickImageCaptchaGenerator() {
    }

    @SneakyThrows
    @Override
    public ImageCaptchaInfo generateCaptchaImage(GenerateParam param) {
        // 文字点选验证码不需要模板 只需要背景图
        Collection<InputStream> inputStreams = new LinkedList<>();
        try {
            Resource resourceImage = getImageResourceManager().randomGetResource(param.getType());
            InputStream resourceInputStream = getImageResourceManager().getResourceInputStream(resourceImage);
            inputStreams.add(resourceInputStream);
            BufferedImage bgImage = CaptchaImageUtils.wrapFile2BufferedImage(resourceInputStream);

            List<ClickImageCheckDefinition> clickImageCheckDefinitionList = new ArrayList<>(interferenceCount);
            int allImages = interferenceCount + checkClickCount;
            int avg = bgImage.getWidth() / allImages;
            for (int i = 0; i < allImages; i++) {
                // 随机获取点击图片
                ImgWrapper imgWrapper = randomGetClickImg();
                BufferedImage image = imgWrapper.getImage();
                int clickImgWidth = image.getWidth();
                int clickImgHeight = image.getHeight();
                // 随机x
                int randomX;
                if (i == 0) {
                    randomX = 1;
                } else {
                    randomX = avg * i;
                }
                // 随机y
                int randomY = ThreadLocalRandom.current().nextInt(10, bgImage.getHeight() - clickImgHeight);
                // 通过随机x和y 进行覆盖图片
                CaptchaImageUtils.overlayImage(bgImage, imgWrapper.getImage(), randomX, randomY);
                ClickImageCheckDefinition clickImageCheckDefinition = new ClickImageCheckDefinition();
                clickImageCheckDefinition.setTip(imgWrapper.getTip());
                clickImageCheckDefinition.setX(randomX + clickImgWidth / 2);
                clickImageCheckDefinition.setY(randomY + clickImgHeight / 2);
                clickImageCheckDefinition.setWidth(clickImgWidth);
                clickImageCheckDefinition.setHeight(clickImgHeight);
                clickImageCheckDefinitionList.add(clickImageCheckDefinition);
            }
            // 打乱
            Collections.shuffle(clickImageCheckDefinitionList);
            // 拿出参与校验的数据
            List<ClickImageCheckDefinition> checkClickImageCheckDefinitionList = new ArrayList<>(checkClickCount);
            for (int i = 0; i < checkClickCount; i++) {
                ClickImageCheckDefinition clickImageCheckDefinition = clickImageCheckDefinitionList.get(i);
                checkClickImageCheckDefinitionList.add(clickImageCheckDefinition);
            }
            // 将校验的文字生成提示图片
            ImgWrapper tipImage = genTipImage(checkClickImageCheckDefinitionList);
            return wrapClickImageCaptchaInfo(param, bgImage, tipImage.getImage(), checkClickImageCheckDefinitionList);

        } finally {
            // 使用完后关闭流
            for (InputStream inputStream : inputStreams) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 随机获取点击的图片
     *
     * @return ImgWrapper
     */
    public abstract ImgWrapper randomGetClickImg();

    /**
     * 生成 tip 图片
     *
     * @param imageCheckDefinitions imageCheckDefinitions
     * @return ImgWrapper
     */
    public abstract ImgWrapper genTipImage(List<ClickImageCheckDefinition> imageCheckDefinitions);

    /**
     * 包装 ImageCaptchaInfo
     *
     * @param param                              param
     * @param bgImage                            bgImage
     * @param tipImage                           tipImage
     * @param checkClickImageCheckDefinitionList checkClickImageCheckDefinitionList
     * @return ImageCaptchaInfo
     */
    public abstract ImageCaptchaInfo wrapClickImageCaptchaInfo(GenerateParam param, BufferedImage bgImage,
                                                               BufferedImage tipImage,
                                                               List<ClickImageCheckDefinition> checkClickImageCheckDefinitionList);

    /**
     * @Author: 天爱有情
     * @date 2022/4/28 14:26
     * @Description 点击图片包装
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImgWrapper {
        /** 图片. */
        private BufferedImage image;
        /** 提示. */
        private String tip;
    }
}
