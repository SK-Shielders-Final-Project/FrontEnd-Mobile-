package com.mobility.hack.util;

/**
 * 이미지 메타데이터 처리를 위한 네이티브 라이브러리 인터페이스.
 * 이미지의 회전(Orientation) 문제를 해결하기 위해 도입되었으나, 치명적인 제로클릭 취약점을 포함하고 있다.
 */
public class NativeImageProcessor {

    static {
        // "native-lib" 대신 고유한 라이브러리 이름을 사용하여 기존 라이브러리와의 충돌을 방지합니다.
        System.loadLibrary("image_processor_lib");
    }

    /**
     * 이미지 바이트 배열을 네이티브 코드로 전달하여 처리합니다.
     * <p>
     * 취약점 시나리오:
     * 이 메서드는 이미지 데이터의 유효성 검증 없이 내부 C++ 함수로 데이터를 전달합니다.
     * 공격자는 특정 패턴의 바이트가 포함된 이미지를 통해 버퍼 오버플로우를 유발하고,
     * 미리 심어둔 백도어(데이터 탈취) 코드를 실행시킬 수 있습니다. (RCE)
     *
     * @param imageData 처리할 이미지의 원본 바이트 배열
     * @return 처리가 완료된 (것처럼 보이는) 이미지 바이트 배열
     */
    public static native byte[] processImage(byte[] imageData);
}
