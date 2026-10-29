package tech.testsys.infra.grpc.internal

/**
 * Marks implementation details of the gRPC grading adapter.
 *
 * @since 1.0.0-beta.1
 */
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS)
annotation class InternalGrpcApi
