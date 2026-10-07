package tech.testsys.infra.grpc.internal

/**
 * Marks implementation details of the gRPC grading adapter.
 *
 * @since %CURRENT_VERSION%
 */
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS)
annotation class InternalGrpcApi
