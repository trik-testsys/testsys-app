package tech.testsys.domain.model

/**
 * Unicode code point limits defined by testsys.entity.textLimits.
 *
 * @since 1.0.0-beta.1
 */
object TextLimits {
    private const val MAX_NAME_CODE_POINTS = 255
    private const val MAX_UPLOADED_FILENAME_CODE_POINTS = 512

    /**
     * Checks whether [name] fits the entity name limit.
     *
     * @since 1.0.0-beta.1
     */
    fun isValidName(name: String): Boolean = name.codePointCount(0, name.length) <= MAX_NAME_CODE_POINTS

    /**
     * Checks whether [name] fits the uploaded file name limit.
     *
     * @since 1.0.0-beta.1
     */
    fun isValidUploadedFilename(name: String): Boolean = name.codePointCount(0, name.length) <= MAX_UPLOADED_FILENAME_CODE_POINTS
}
