package tech.testsys.domain.model

/**
 * Unicode code point limits defined by testsys.entity.textLimits.
 *
 * @since %CURRENT_VERSION%
 */
object TextLimits {
    private const val MAX_NAME_CODE_POINTS = 255
    private const val MAX_UPLOADED_FILENAME_CODE_POINTS = 512

    /**
     * Checks whether [name] fits the entity name limit.
     *
     * @since %CURRENT_VERSION%
     */
    fun isValidName(name: String): Boolean = name.codePointCount(0, name.length) <= MAX_NAME_CODE_POINTS

    /**
     * Checks whether [name] fits the uploaded file name limit.
     *
     * @since %CURRENT_VERSION%
     */
    fun isValidUploadedFilename(name: String): Boolean = name.codePointCount(0, name.length) <= MAX_UPLOADED_FILENAME_CODE_POINTS
}
