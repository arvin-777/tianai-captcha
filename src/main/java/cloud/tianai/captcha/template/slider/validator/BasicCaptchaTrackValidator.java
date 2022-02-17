package cloud.tianai.captcha.template.slider.validator;

import cloud.tianai.captcha.template.slider.util.CollectionUtils;
import cloud.tianai.captcha.template.slider.util.ObjectUtils;

import java.util.List;

/**
 * @Author: 天爱有情
 * @date 2022/2/17 11:01
 * @Description 基本的行为轨迹校验
 */
public class BasicCaptchaTrackValidator extends SimpleSliderCaptchaValidator {

    public BasicCaptchaTrackValidator() {
    }

    public BasicCaptchaTrackValidator(float defaultTolerant) {
        super(defaultTolerant);
    }

    @Override
    public boolean valid(SliderCaptchaTrack sliderCaptchaTrack, Float oriPercentage) {
        // 校验参数
        checkParam(sliderCaptchaTrack);
        // 基础校验
        boolean superValid = super.valid(sliderCaptchaTrack, oriPercentage);
        if (!superValid) {
            return false;
        }
        // 进行行为轨迹检测
        long startSlidingTime = sliderCaptchaTrack.getStartSlidingTime().getTime();
        long endSlidingTime = sliderCaptchaTrack.getEntSlidingTime().getTime();
        Integer bgImageWidth = sliderCaptchaTrack.getBgImageWidth();
        List<SliderCaptchaTrack.Track> trackList = sliderCaptchaTrack.getTrackList();
        // 这里只进行基本检测, 用一些简单算法进行校验，如有需要可扩展
        // 检测1: 滑动时间如果小于300毫秒 返回false
        // 检测2: 轨迹数据要是少于背景宽度的五分之一，或者大于背景宽度的五分之一 返回false
        // 检测3: x轴和y轴应该是从0开始的，要是一开始x轴和y轴乱跑，返回false
        // 检测4: 如果y轴是相同的，必然是机器操作，直接返回false
        // 检测5： x轴或者y轴直接的区间跳跃过大的话返回 false
        // 检测6: x轴应该是由快到慢的， 要是速率一致，返回false
        // 检测7: 如果x轴超过图片宽度的频率过高，返回false

        // 检测1
        if (startSlidingTime + 300 > endSlidingTime) {
            return false;
        }
        // 检测2
        if (trackList.size() < bgImageWidth / 5 || trackList.size() > bgImageWidth * 5) {
            return false;
        }
        // 检测3
        SliderCaptchaTrack.Track firstTrack = trackList.get(0);
        if (firstTrack.getX() > 1 || firstTrack.getX() < -2 || firstTrack.getY() > 1 || firstTrack.getY() < -2) {
            return false;
        }
        int check4 = 0;
        int check7 = 0;
        for (int i = 1; i < trackList.size(); i++) {
            SliderCaptchaTrack.Track track = trackList.get(i);
            int x = track.getX();
            int y = track.getY();
            // check4
            if (firstTrack.getY() == y) {
                check4++;
            }
            // check7
            if (x >= bgImageWidth) {
                check7++;
            }
            // check5
            SliderCaptchaTrack.Track preTrack = trackList.get(i - 1);
            if ((track.getX() - preTrack.getX()) > 5 || (track.getY() - preTrack.getY()) > 5) {
                return false;
            }
        }
        if (check4 > trackList.size() * 0.7 || check7 > 200) {
            return false;
        }

        // check6
        int splitPos = (int) (trackList.size() * 0.7);
        SliderCaptchaTrack.Track splitPostTrack = trackList.get(splitPos - 1);
        int posTime = splitPostTrack.getT();
        float startAvgPosTime = posTime / (float) splitPos;

        SliderCaptchaTrack.Track lastTrack = trackList.get(trackList.size() - 1);
        float endAvgPosTime = lastTrack.getT() / (float) (trackList.size() - splitPos);

        return endAvgPosTime > startAvgPosTime;
    }


    public void checkParam(SliderCaptchaTrack sliderCaptchaTrack) {
        if (ObjectUtils.isEmpty(sliderCaptchaTrack.getBgImageWidth())) {
            throw new IllegalArgumentException("bgImageWidth must not be null");
        }
        if (ObjectUtils.isEmpty(sliderCaptchaTrack.getBgImageHeight())) {
            throw new IllegalArgumentException("bgImageHeight must not be null");
        }
        if (ObjectUtils.isEmpty(sliderCaptchaTrack.getSliderImageWidth())) {
            throw new IllegalArgumentException("sliderImageWidth must not be null");
        }
        if (ObjectUtils.isEmpty(sliderCaptchaTrack.getSliderImageHeight())) {
            throw new IllegalArgumentException("sliderImageHeight must not be null");
        }
        if (ObjectUtils.isEmpty(sliderCaptchaTrack.getStartSlidingTime())) {
            throw new IllegalArgumentException("startSlidingTime must not be null");
        }
        if (ObjectUtils.isEmpty(sliderCaptchaTrack.getEntSlidingTime())) {
            throw new IllegalArgumentException("entSlidingTime must not be null");
        }
        if (CollectionUtils.isEmpty(sliderCaptchaTrack.getTrackList())) {
            throw new IllegalArgumentException("trackList must not be null");
        }
        for (SliderCaptchaTrack.Track track : sliderCaptchaTrack.getTrackList()) {
            Integer x = track.getX();
            Integer y = track.getY();
            Integer t = track.getT();
            if (x == null || y == null || t == null) {
                throw new IllegalArgumentException("track[x,y,t] must not be null");
            }
        }
    }
}
