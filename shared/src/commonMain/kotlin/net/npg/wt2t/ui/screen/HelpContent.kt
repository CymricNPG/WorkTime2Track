package net.npg.wt2t.ui.screen

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.break_time
import worktime2track.shared.generated.resources.closed
import worktime2track.shared.generated.resources.discard
import worktime2track.shared.generated.resources.free_time
import worktime2track.shared.generated.resources.help_booking_hint
import worktime2track.shared.generated.resources.help_booking_step_1
import worktime2track.shared.generated.resources.help_booking_step_2
import worktime2track.shared.generated.resources.help_booking_step_3
import worktime2track.shared.generated.resources.help_booking_step_4
import worktime2track.shared.generated.resources.help_booking_step_5
import worktime2track.shared.generated.resources.help_booking_title
import worktime2track.shared.generated.resources.help_edit_day_hint
import worktime2track.shared.generated.resources.help_edit_day_step_1
import worktime2track.shared.generated.resources.help_edit_day_step_2
import worktime2track.shared.generated.resources.help_edit_day_step_3
import worktime2track.shared.generated.resources.help_edit_day_step_4
import worktime2track.shared.generated.resources.help_edit_day_step_5
import worktime2track.shared.generated.resources.help_edit_day_title
import worktime2track.shared.generated.resources.help_end_day_hint
import worktime2track.shared.generated.resources.help_end_day_step_1
import worktime2track.shared.generated.resources.help_end_day_step_2
import worktime2track.shared.generated.resources.help_end_day_step_3
import worktime2track.shared.generated.resources.help_end_day_step_4
import worktime2track.shared.generated.resources.help_end_day_title
import worktime2track.shared.generated.resources.help_tasks_hint
import worktime2track.shared.generated.resources.help_tasks_step_1
import worktime2track.shared.generated.resources.help_tasks_step_2
import worktime2track.shared.generated.resources.help_tasks_step_3
import worktime2track.shared.generated.resources.help_tasks_step_4
import worktime2track.shared.generated.resources.help_tasks_title
import worktime2track.shared.generated.resources.logoff
import worktime2track.shared.generated.resources.note_hint
import worktime2track.shared.generated.resources.overview_title
import worktime2track.shared.generated.resources.pause
import worktime2track.shared.generated.resources.projectless_tasks
import worktime2track.shared.generated.resources.projects_title
import worktime2track.shared.generated.resources.save
import worktime2track.shared.generated.resources.send
import worktime2track.shared.generated.resources.stop_booking
import worktime2track.shared.generated.resources.target_time
import worktime2track.shared.generated.resources.task_name

/** Localized text for one quick-start topic. */
internal data class HelpTopic(val title: String, val steps: List<String>, val hint: String)

/** Resolves the German guide and the GUI labels in the selected application language. */
@Composable
internal fun createHelpTopics(): List<HelpTopic> = listOf(
    HelpTopic(
        title = stringResource(Res.string.help_tasks_title),
        steps = listOf(
            getHelpText(Res.string.help_tasks_step_1, Res.string.projects_title),
            getHelpText(Res.string.help_tasks_step_2, Res.string.projectless_tasks, Res.string.save),
            getHelpText(Res.string.help_tasks_step_3, Res.string.task_name),
            getHelpText(Res.string.help_tasks_step_4, Res.string.save),
        ),
        hint = getHelpText(Res.string.help_tasks_hint, Res.string.free_time, Res.string.closed),
    ),
    HelpTopic(
        title = stringResource(Res.string.help_booking_title),
        steps = listOf(
            getHelpText(Res.string.help_booking_step_1),
            getHelpText(Res.string.help_booking_step_2),
            getHelpText(Res.string.help_booking_step_3, Res.string.stop_booking),
            getHelpText(Res.string.help_booking_step_4, Res.string.note_hint, Res.string.send),
            getHelpText(Res.string.help_booking_step_5),
        ),
        hint = getHelpText(Res.string.help_booking_hint),
    ),
    HelpTopic(
        title = stringResource(Res.string.help_end_day_title),
        steps = listOf(
            getHelpText(Res.string.help_end_day_step_1, Res.string.logoff),
            getHelpText(Res.string.help_end_day_step_2, Res.string.target_time),
            getHelpText(Res.string.help_end_day_step_3, Res.string.break_time),
            getHelpText(Res.string.help_end_day_step_4, Res.string.save),
        ),
        hint = getHelpText(Res.string.help_end_day_hint),
    ),
    HelpTopic(
        title = stringResource(Res.string.help_edit_day_title),
        steps = listOf(
            getHelpText(Res.string.help_edit_day_step_1, Res.string.overview_title),
            getHelpText(Res.string.help_edit_day_step_2),
            getHelpText(Res.string.help_edit_day_step_3),
            getHelpText(Res.string.help_edit_day_step_4, Res.string.target_time, Res.string.pause),
            getHelpText(Res.string.help_edit_day_step_5, Res.string.save, Res.string.discard),
        ),
        hint = getHelpText(Res.string.help_edit_day_hint),
    ),
)

@Composable
private fun getHelpText(resource: StringResource, vararg labels: StringResource): String =
    stringResource(resource, *labels.map { stringResource(it) }.toTypedArray())

