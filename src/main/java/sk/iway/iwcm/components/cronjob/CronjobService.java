package sk.iway.iwcm.components.cronjob;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.cron.CronDB;
import sk.iway.iwcm.system.cron.CronTask;

import java.util.List;

@Service
public class CronjobService {

    Page<CronTask> getCronTasks(Pageable pageable) {
        List<CronTask> cronTaskList = CronDB.getAll();

        for (CronTask cronTask : cronTaskList) {
            if (Tools.isEmpty(cronTask.getTaskName())) {
                fillCronTasksTaskName(cronTask);
            }
        }

        PagedListHolder<CronTask> holder = new PagedListHolder<>(cronTaskList);
        holder.setPageSize(pageable.getPageSize());
        holder.setPage(pageable.getPageNumber());

        return new PageImpl<>(holder.getPageList(), pageable, cronTaskList.size());
    }

    boolean deleteCronTask(Long id) {
        CronDB.delete(id);
        return true;
    }

    CronTask saveCronTask(CronTask cronTask) {
        if (Tools.isEmpty(cronTask.getTaskName())) {
            fillCronTasksTaskName(cronTask);
        }

        cronTask.setId((long) -1);
        return CronDB.save(cronTask);
    }

    CronTask editCronTask(CronTask cronTask, long id) {
        if (Tools.isEmpty(cronTask.getTaskName())) {
            fillCronTasksTaskName(cronTask);
        }

        cronTask.setId(id);
        CronDB.save(cronTask);
        return cronTask;
    }

    private void fillCronTasksTaskName(CronTask cronTask) {
        String[] taskArray = cronTask.getTask().split("\\.");
        String className = taskArray[taskArray.length - 1];
        cronTask.setTaskName(className);
    }
}