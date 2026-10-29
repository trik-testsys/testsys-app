package tech.testsys.infra.settings.api.catalog

/**
 * Closed catalog of persisted settings; durations use ISO-8601 and strings retain whitespace. The catalog has no
 * defaults: the module owning a key sets its default in Environment.
 *
 * @property property the exact Environment key.
 * @property kind the serialized value type.
 * @property isRestartRequired whether a saved value takes effect only in a new process.
 * @since 1.0.0-beta.1
 */
enum class SettingKey(
    val property: String,
    val kind: SettingKind,
    val isRestartRequired: Boolean = false,
) {
    /**
     * Maximum number of competition participants.
     *
     * @since 1.0.0-beta.1
     */
    MaxParticipants("testsys.operation.competition.max-participants", SettingKind.Integer),

    /**
     * Lifetime captured when issuing a confirmation code.
     *
     * @since 1.0.0-beta.1
     */
    ConfirmationCodeLifetime("testsys.operation.email-confirmation.confirmation-code-lifetime", SettingKind.Duration),

    /**
     * Attempt count captured when issuing a confirmation code.
     *
     * @since 1.0.0-beta.1
     */
    MaxConfirmationAttempts("testsys.operation.email-confirmation.max-confirmation-attempts", SettingKind.Integer),

    /**
     * Validity of newly created or renewed community invitations.
     *
     * @since 1.0.0-beta.1
     */
    CommunityInviteTtl("testsys.operation.community-invite.ttl", SettingKind.Duration),

    /**
     * Validity of newly created or renewed class invitations.
     *
     * @since 1.0.0-beta.1
     */
    ClassInviteTtl("testsys.operation.class-invite.ttl", SettingKind.Duration),

    /**
     * Transport attempts captured when accepting a grading run.
     *
     * @since 1.0.0-beta.1
     */
    GradingMaxAttempts("testsys.grading.max-attempts", SettingKind.Integer),

    /**
     * Total deadline captured when accepting a grading run.
     *
     * @since 1.0.0-beta.1
     */
    TotalTimeout("testsys.grading.total-timeout", SettingKind.Duration),

    /**
     * Individual RPC deadline captured when accepting a grading run.
     *
     * @since 1.0.0-beta.1
     */
    RpcTimeout("testsys.grading.rpc-timeout", SettingKind.Duration),

    /**
     * Recording choice captured when accepting a grading run.
     *
     * @since 1.0.0-beta.1
     */
    ShouldRecordVideo("testsys.grading.should-record-video", SettingKind.Boolean),

    /**
     * Interval of the currently published polling schedule.
     *
     * @since 1.0.0-beta.1
     */
    PollInterval("testsys.grading.poll-interval", SettingKind.Duration),

    /**
     * Deadline captured by each new status RPC.
     *
     * @since 1.0.0-beta.1
     */
    StatusTimeout("testsys.grading.status-timeout", SettingKind.Duration),

    /**
     * Saved inbound size limit applied at process startup.
     *
     * @since 1.0.0-beta.1
     */
    MaxMessageBytes("testsys.grading.max-message-bytes", SettingKind.Integer, isRestartRequired = true),

    /**
     * Inclusive maximum captured by each diagnostic call.
     *
     * @since 1.0.0-beta.1
     */
    MaxTimeLimitMillis("testsys.diagnostics.max-time-limit-millis", SettingKind.Long),

    /**
     * Score output text captured by each diagnostic call.
     *
     * @since 1.0.0-beta.1
     */
    ScorePrefix("testsys.diagnostics.score-prefix", SettingKind.Text),

    /**
     * Minimum blob age captured by an explicit cleanup pass.
     *
     * @since 1.0.0-beta.1
     */
    CleanupMinimumAge("testsys.file-storage.cleanup.minimum-age", SettingKind.Duration),

    /**
     * Batch limit captured by an explicit cleanup pass.
     *
     * @since 1.0.0-beta.1
     */
    CleanupBatchSize("testsys.file-storage.cleanup.batch-size", SettingKind.Integer),
}

/**
 * Value representations accepted by the settings catalog.
 *
 * @since 1.0.0-beta.1
 */
enum class SettingKind {
    /**
     * Signed decimal integer.
     *
     * @since 1.0.0-beta.1
     */
    Integer,

    /**
     * Signed decimal long integer.
     *
     * @since 1.0.0-beta.1
     */
    Long,

    /**
     * Strict lowercase boolean.
     *
     * @since 1.0.0-beta.1
     */
    Boolean,

    /**
     * ISO-8601 duration.
     *
     * @since 1.0.0-beta.1
     */
    Duration,

    /**
     * Untrimmed nonblank text.
     *
     * @since 1.0.0-beta.1
     */
    Text,
}
