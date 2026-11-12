package tech.testsys.web.app.view

/**
 * Form of a details block whose fields a load builds: the `editing` switch of the block calls [save] and [cancel]
 * of the latest loaded form.
 *
 * @property save checks and saves the values; returns whether the block leaves editing.
 * @property cancel restores the loaded values.
 */
internal class DetailsForm(val save: () -> Boolean, val cancel: () -> Unit)
