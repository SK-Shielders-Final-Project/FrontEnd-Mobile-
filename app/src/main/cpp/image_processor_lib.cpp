#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <cstdint> // uint16_t를 사용하기 위해 추가

// 이 함수는 EXIF 데이터의 특정 필드 길이를 읽어 해당 길이만큼 데이터를 처리하는 척합니다.
// 하지만 길이 값을 검증하지 않아 버퍼 오버플로우에 취약합니다.
void processVulnerableExifChunk(const std::vector<unsigned char>& data, size_t offset) {
    // 2바이트를 읽어서 처리할 데이터의 길이라고 가정합니다.
    // [취약점] 공격자는 이 길이 값을 (예: 65535) 비정상적으로 크게 조작할 수 있습니다.
    uint16_t chunk_length = (data[offset] << 8) | data[offset + 1];

    if (chunk_length > 1000) { // 비정상적으로 큰 길이 값 감지 (실제로는 이런 방어 코드도 없었을 것)
        // [취약점 지점]
        // 비정상적인 길이 값이 감지되었습니다. 정상적인 프로그램이라면 여기서 처리를 중단해야 합니다.
        // 하지만 이 코드는 길이 값을 그대로 믿고 처리를 강행하려고 시도하며,
        // 이로 인해 버퍼 오버플로우가 발생하여 공격자가 원하는 코드를 실행할 수 있게 됩니다.
        //
        // 시연을 위해 현재는 비워져 있으며, 공격 코드는 여기에 작성됩니다.
        //
        // e.g., memcpy(destination_buffer, &data[offset + 2], chunk_length); // <-- BOOM! CRASH!
    } else {
        // 정상적인 길이라면 메타데이터를 처리하는 로직이 여기에 위치했을 것입니다.
    }
}


/**
 * JNI 함수: 이미지의 EXIF 데이터를 파싱하여 회전 값을 찾는 것처럼 동작합니다.
 * 하지만 EXIF 데이터의 구조(특히 길이 필드)를 신뢰하여 메모리 손상에 취약합니다.
 */
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_mobility_hack_util_NativeImageProcessor_processImage(
        JNIEnv *env,
        jclass,
        jbyteArray image_data) {

    jbyte *imageBytes = env->GetByteArrayElements(image_data, nullptr);
    jsize length = env->GetArrayLength(image_data);
    std::vector<unsigned char> data(imageBytes, imageBytes + length);

    // EXIF 데이터는 보통 'JFIF' 마커 바로 뒤에 위치합니다.
    // 여기서는 시나리오의 단순화를 위해, 이미지의 특정 오프셋(예: 20번째 바이트)부터
    // EXIF 데이터가 시작된다고 가정합니다.
    if (data.size() > 22) {
        // 취약한 EXIF 처리 함수를 호출합니다.
        processVulnerableExifChunk(data, 20);
    }

    env->ReleaseByteArrayElements(image_data, imageBytes, JNI_ABORT);
    return image_data;
}
