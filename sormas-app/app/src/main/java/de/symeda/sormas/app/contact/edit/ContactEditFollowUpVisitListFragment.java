package de.symeda.sormas.app.contact.edit;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import de.symeda.sormas.api.contact.ContactClassification;
import de.symeda.sormas.app.BaseEditActivityFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.contact.Contact;
import de.symeda.sormas.app.backend.visit.Visit;
import de.symeda.sormas.app.contact.edit.sub.ContactEditFollowUpInfoActivity;
import de.symeda.sormas.app.core.BoolResult;
import de.symeda.sormas.app.core.IActivityCommunicator;
import de.symeda.sormas.app.core.adapter.databinding.OnListItemClickListener;
import de.symeda.sormas.app.core.async.DefaultAsyncTask;
import de.symeda.sormas.app.core.async.ITaskResultCallback;
import de.symeda.sormas.app.core.async.ITaskResultHolderIterator;
import de.symeda.sormas.app.core.async.TaskResultHolder;
import de.symeda.sormas.app.databinding.FragmentFormListLayoutBinding;
import de.symeda.sormas.app.rest.SynchronizeDataAsync;
import de.symeda.sormas.app.shared.ContactFormFollowUpNavigationCapsule;
import de.symeda.sormas.app.shared.ContactFormNavigationCapsule;

/**
 * Created by Orson on 13/02/2018.
 * <p>
 * www.technologyboard.org
 * sampson.orson@gmail.com
 * sampson.orson@technologyboard.org
 */

public class ContactEditFollowUpVisitListFragment extends BaseEditActivityFragment<FragmentFormListLayoutBinding, List<Visit>, Contact> implements OnListItemClickListener {

    private AsyncTask onResumeTask;
    private String recordUuid;
    private ContactClassification pageStatus = null;
    private List<Visit> record;
    private FragmentFormListLayoutBinding binding;

    private ContactEditFollowupListAdapter adapter;
    private LinearLayoutManager linearLayoutManager;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        savePageStatusState(outState, pageStatus);
        saveRecordUuidState(outState, recordUuid);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = (savedInstanceState != null) ? savedInstanceState : getArguments();

        recordUuid = getRecordUuidArg(arguments);
        pageStatus = (ContactClassification) getPageStatusArg(arguments);
    }

    @Override
    protected String getSubHeadingTitle() {
        Resources r = getResources();
        return r.getString(R.string.caption_visit_information);
    }

    @Override
    public List<Visit> getPrimaryData() {
        return record;
    }

    @Override
    public boolean onBeforeLayoutBinding(Bundle savedInstanceState, TaskResultHolder resultHolder, BoolResult resultStatus, boolean executionComplete) {
        if (!executionComplete) {
            Contact contact = getActivityRootData();
            List<Visit> visitList = new ArrayList<Visit>();

            //Case caze = DatabaseHelper.getCaseDao().queryUuidReference(recordUuid);
            if (contact != null) {
                if (contact.isUnreadOrChildUnread())
                    DatabaseHelper.getContactDao().markAsRead(contact);

                visitList = DatabaseHelper.getVisitDao().getByContact(contact);
            }

            resultHolder.forList().add(visitList);
        } else {
            ITaskResultHolderIterator listIterator = resultHolder.forList().iterator();
            if (listIterator.hasNext()) {
                record = listIterator.next();

            }
        }

        return true;
    }

    @Override
    public void onLayoutBinding(FragmentFormListLayoutBinding contentBinding) {
        showEmptyListHint(record, R.string.entity_visit);

        adapter = new ContactEditFollowupListAdapter(this.getActivity(), R.layout.row_read_followup_list_item_layout, this, record);

        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        contentBinding.recyclerViewForList.setLayoutManager(linearLayoutManager);
        contentBinding.recyclerViewForList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onAfterLayoutBinding(FragmentFormListLayoutBinding contentBinding) {

    }

    @Override
    protected void updateUI(FragmentFormListLayoutBinding contentBinding, List<Visit> visits) {

    }

    @Override
    public void onPageResume(FragmentFormListLayoutBinding contentBinding, boolean hasBeforeLayoutBindingAsyncReturn) {
        final SwipeRefreshLayout swiperefresh = (SwipeRefreshLayout) this.getView().findViewById(R.id.swiperefresh);
        if (swiperefresh != null) {
            swiperefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getActivityCommunicator().synchronizeData(SynchronizeDataAsync.SyncMode.Changes, true, false, true, swiperefresh, null);
                }
            });
        }

        if (!hasBeforeLayoutBindingAsyncReturn)
            return;

        DefaultAsyncTask executor = new DefaultAsyncTask(getContext()) {
            @Override
            public void onPreExecute() {
                //getActivityCommunicator().showPreloader();
                //getActivityCommunicator().hideFragmentView();
            }

            @Override
            public void execute(TaskResultHolder resultHolder) {
                Contact contact = getActivityRootData();
                List<Visit> visitList = new ArrayList<Visit>();

                //Case caze = DatabaseHelper.getCaseDao().queryUuidReference(recordUuid);
                if (contact != null) {
                    if (contact.isUnreadOrChildUnread())
                        DatabaseHelper.getContactDao().markAsRead(contact);

                    visitList = DatabaseHelper.getVisitDao().getByContact(contact);
                }

                resultHolder.forList().add(visitList);
            }
        };
        onResumeTask = executor.execute(new ITaskResultCallback() {
            @Override
            public void taskResult(BoolResult resultStatus, TaskResultHolder resultHolder) {
                //getActivityCommunicator().hidePreloader();
                //getActivityCommunicator().showFragmentView();

                if (resultHolder == null) {
                    return;
                }

                ITaskResultHolderIterator listIterator = resultHolder.forList().iterator();
                if (listIterator.hasNext())
                    record = listIterator.next();

                requestLayoutRebind();
            }
        });
    }

    @Override
    public int getRootEditLayout() {
        return R.layout.fragment_root_list_form_layout;
    }

    @Override
    public int getEditLayout() {
        return R.layout.fragment_form_list_layout;
    }

    @Override
    public boolean includeFabNonOverlapPadding() {
        return false;
    }

    @Override
    public boolean showSaveAction() {
        return false;
    }

    @Override
    public boolean showAddAction() {
        return false;
    }

    @Override
    public void onListItemClick(View view, int position, Object item) {
        Visit record = (Visit) item;
        ContactFormFollowUpNavigationCapsule dataCapsule = (ContactFormFollowUpNavigationCapsule) new ContactFormFollowUpNavigationCapsule(getContext(),
                record.getUuid(), record.getVisitStatus()).setContactUuid(recordUuid);
        ContactEditFollowUpInfoActivity.goToActivity(getActivity(), dataCapsule);
    }

    public static ContactEditFollowUpVisitListFragment newInstance(IActivityCommunicator activityCommunicator, ContactFormNavigationCapsule capsule, Contact activityRootData) {
        return newInstance(activityCommunicator, ContactEditFollowUpVisitListFragment.class, capsule, activityRootData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (onResumeTask != null && !onResumeTask.isCancelled())
            onResumeTask.cancel(true);
    }

}