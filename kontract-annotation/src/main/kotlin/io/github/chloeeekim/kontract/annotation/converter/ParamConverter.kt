package io.github.chloeeekim.kontract.annotation.converter

/**
 * String 값을 커스텀 타입으로 변환하는 인터페이스.
 * @TypeConverter와 함께 사용하여 기본 지원 타입(String, Int, Long, Enum) 외의 타입을 파싱할 수 있다.
 *
 * 구현체는 반드시 인자 없는 기본 생성자를 가져야 한다.
 */
interface ParamConverter<T> {
    fun convert(value: String): T
}