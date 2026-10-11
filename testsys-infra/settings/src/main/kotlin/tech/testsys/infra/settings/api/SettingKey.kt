package tech.testsys.infra.settings.api

/**
 * Closed catalog of persisted settings; durations use ISO-8601 and strings retain whitespace.
 *
 * @property property the exact Environment key.
 * @property kind the serialized value type.
 * @property defaultValue the fallback when neither an override nor Environment value exists.
 * @property isRestartRequired whether a saved value takes effect only in a new process.
 * @since %CURRENT_VERSION%
 */
enum class SettingKey(
    val property: String,
    val kind: SettingKind,
    val defaultValue: String? = null,
    val isRestartRequired: Boolean = false,
) {
    /**
     * Maximum number of competition participants.
     *
     * @since %CURRENT_VERSION%
     */
    MaxParticipants("testsys.operation.competition.max-participants", SettingKind.Integer),

    /**
     * Lifetime captured when issuing a confirmation code.
     *
     * @since %CURRENT_VERSION%
     */
    ConfirmationCodeLifetime("testsys.operation.email-confirmation.confirmation-code-lifetime", SettingKind.Duration),

    /**
     * Attempt count captured when issuing a confirmation code.
     *
     * @since %CURRENT_VERSION%
     */
    MaxConfirmationAttempts("testsys.operation.email-confirmation.max-confirmation-attempts", SettingKind.Integer),

    /**
     * Validity of newly created or renewed community invitations.
     *
     * @since %CURRENT_VERSION%
     */
    CommunityInviteTtl("testsys.operation.community-invite.ttl", SettingKind.Duration),

    /**
     * Validity of newly created or renewed class invitations.
     *
     * @since %CURRENT_VERSION%
     */
    ClassInviteTtl("testsys.operation.class-invite.ttl", SettingKind.Duration),

    /**
     * Transport attempts captured when accepting a grading run.
     *
     * @since %CURRENT_VERSION%
     */
    GradingMaxAttempts("testsys.grading.max-attempts", SettingKind.Integer, "3"),

    /**
     * Total deadline captured when accepting a grading run.
     *
     * @since %CURRENT_VERSION%
     */
    TotalTimeout("testsys.grading.total-timeout", SettingKind.Duration, "PT30M"),

    /**
     * Individual RPC deadline captured when accepting a grading run.
     *
     * @since %CURRENT_VERSION%
     */
    RpcTimeout("testsys.grading.rpc-timeout", SettingKind.Duration, "PT10M"),

    /**
     * Recording choice captured when accepting a grading run.
     *
     * @since %CURRENT_VERSION%
     */
    ShouldRecordVideo("testsys.grading.should-record-video", SettingKind.Boolean, "true"),

    /**
     * Interval of the currently published polling schedule.
     *
     * @since %CURRENT_VERSION%
     */
    PollInterval("testsys.grading.poll-interval", SettingKind.Duration, "PT5S"),

    /**
     * Deadline captured by each new status RPC.
     *
     * @since %CURRENT_VERSION%
     */
    StatusTimeout("testsys.grading.status-timeout", SettingKind.Duration, "PT5S"),

    /**
     * Saved inbound size limit applied at process startup.
     *
     * @since %CURRENT_VERSION%
     */
    MaxMessageBytes("testsys.grading.max-message-bytes", SettingKind.Integer, "400000000", isRestartRequired = true),

    /**
     * Inclusive maximum captured by each diagnostic call.
     *
     * @since %CURRENT_VERSION%
     */
    MaxTimeLimitMillis("testsys.diagnostics.max-time-limit-millis", SettingKind.Long, "600000"),

    /**
     * Score output text captured by each diagnostic call.
     *
     * @since %CURRENT_VERSION%
     */
    ScorePrefix("testsys.diagnostics.score-prefix", SettingKind.Text, "Набрано баллов:"),

    /**
     * Minimum blob age captured by an explicit cleanup pass.
     *
     * @since %CURRENT_VERSION%
     */
    CleanupMinimumAge("testsys.file-storage.cleanup.minimum-age", SettingKind.Duration, "PT24H"),

    /**
     * Batch limit captured by an explicit cleanup pass.
     *
     * @since %CURRENT_VERSION%
     */
    CleanupBatchSize("testsys.file-storage.cleanup.batch-size", SettingKind.Integer, "500"),
}

/**
 * Value representations accepted by the settings catalog.
 *
 * @since %CURRENT_VERSION%
 */
enum class SettingKind {
    /**
     * Signed decimal integer.
     *
     * @since %CURRENT_VERSION%
     */
    Integer,

    /**
     * Signed decimal long integer.
     *
     * @since %CURRENT_VERSION%
     */
    Long,

    /**
     * Strict lowercase boolean.
     *
     * @since %CURRENT_VERSION%
     */
    Boolean,

    /**
     * ISO-8601 duration.
     *
     * @since %CURRENT_VERSION%
     */
    Duration,

    /**
     * Untrimmed nonblank text.
     *
     * @since %CURRENT_VERSION%
     */
    Text,
}
