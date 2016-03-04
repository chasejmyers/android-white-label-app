package com.votinginfoproject.VotingInformationProject.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.votinginfoproject.VotingInformationProject.R;
import com.votinginfoproject.VotingInformationProject.activities.VIPTabBarActivity;
import com.votinginfoproject.VotingInformationProject.adapters.CandidatesAdapter;
import com.votinginfoproject.VotingInformationProject.models.Contest;
import com.votinginfoproject.VotingInformationProject.models.VoterInfo;
import com.votinginfoproject.VotingInformationProject.models.singletons.UserPreferences;

import java.util.ArrayList;

import static butterknife.ButterKnife.findById;

public class ContestFragment extends Fragment {
    private final String TAG = ContestFragment.class.getSimpleName();

    private static final String CONTEST_NUM = "contest_number";
    private int contestNum;
    Contest contest;
    private ViewGroup mContainer;

    private ListView mListView;

    private TextView mTitle;
    private TextView mSubtitle;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param contest_number Index of this contest within the list of contests on VoterInfo object
     * @return A new instance of fragment ContestFragment.
     */
    public static ContestFragment newInstance(int contest_number) {
        ContestFragment fragment = new ContestFragment();
        Bundle args = new Bundle();
        args.putInt(CONTEST_NUM, contest_number);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Hide ballot fragment components here, then show them again when user goes back.
     * Doing this because replacing a fragment within a TabBar doesn't remove the old fragment.
     * Ballot layout has its contents wrapped in an inner RelativeLayout so there is only one
     * child view to hide/show here.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContainer = container;
        Log.d(TAG + ":onCreateView", "Hiding ballot container's view");
        mContainer.getChildAt(0).setVisibility(View.INVISIBLE);

        View inflaterView = inflater.inflate(R.layout.fragment_contest, null, false);

        mListView = (ListView) inflaterView.findViewById(R.id.contest_candidate_list);
        mTitle = (TextView) inflaterView.findViewById(R.id.contest_title);
        mSubtitle = (TextView) inflaterView.findViewById(R.id.contest_subtitle);

        if (getArguments() != null) {
            contestNum = getArguments().getInt(CONTEST_NUM);
            Log.d(TAG, "Got contest #" + contestNum);
        }

        setContents();

        return inflaterView;
    }

    public ContestFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG, "In onActivityCreated");
    }

    /**
     * Helper function to populate the view labels.
     */
    private void setContents() {
        final VIPTabBarActivity myActivity = (VIPTabBarActivity) getActivity();


        try {
            ArrayList<Contest> contests = new ArrayList<>();
            VoterInfo voterInfo = UserPreferences.getVoterInfo();

            contests.addAll(voterInfo.getFilteredContestsForParty(UserPreferences.getSelectedParty()));
            contest = contests.get(contestNum);
            Log.d(TAG, "Got contest for office: " + contest.office);

            // title / subtitle is referendumTitle and referendumSubtitle, if election is
            // of type 'Referendum'; else title is office and subtitle is election name
            if (!contest.type.equals("Referendum")) {
                mTitle.setText(contest.office);
                mSubtitle.setText(voterInfo.election.name);

                // add footer view for feedback
                View feedback_layout = myActivity.getLayoutInflater().inflate(R.layout.feedback_link, mListView, false);

                // 'false' argument here is to make the footer list item not clickable (text instead is clickable)
                mListView.addFooterView(feedback_layout, null, false);
            } else {
                // Have a referendum, which has no candidates list.  So,
                // remove scrolling list view and instead use scroll view for title/subtitle.
                // Scroll view includes feedback link at the bottom.
                ViewGroup parent = (ViewGroup) myActivity.findViewById(R.id.contest_fragment);
                parent.removeView(myActivity.findViewById(R.id.contest_fragment_inner_layout));
                View scroll = myActivity.getLayoutInflater().inflate(R.layout.contest_referendum, parent, false);
                parent.addView(scroll);

                // go find references to the TextViews in the new ScrollView
                mTitle = (TextView) myActivity.findViewById(R.id.contest_title);
                mSubtitle = (TextView) myActivity.findViewById(R.id.contest_subtitle);

                TextView referendumText = findById(scroll, R.id.contest_referendum_text);
                TextView referendumPro = findById(scroll, R.id.contest_referendum_pro);
                TextView referendumCon = findById(scroll, R.id.contest_referendum_con);

                // deal with huge referendum descriptions by reducing font size a bit.
                if (contest.referendumTitle != null) {
                    mTitle.setText(contest.referendumTitle);

                    if (contest.referendumTitle.length() > 20) {
                        mTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getActivity().getResources().getDimension(R.dimen.text_small));
                    }
                }

                if (contest.referendumSubtitle != null && !contest.referendumSubtitle.isEmpty()) {
                    mSubtitle.setText(contest.referendumSubtitle);

                    if (contest.referendumSubtitle.length() > 20) {
                        mSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getActivity().getResources().getDimension(R.dimen.text_small));
                    }
                } else {
                    mSubtitle.setVisibility(View.GONE);
                }

                referendumText.setText(contest.referendumText);
                referendumPro.setText(contest.referendumProStatement);
                referendumCon.setText(contest.referendumConStatement);
            }

            // populate candidate list
            if (contest.candidates != null && !contest.candidates.isEmpty()) {
                CandidatesAdapter adapter = new CandidatesAdapter(myActivity, contest.candidates);
                mListView.setAdapter(adapter);
                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.d(TAG + ":List", "clicked: " + contest.candidates.get(position).name);

                        myActivity.showCandidateDetails(contestNum, position);
                    }
                });
            } else if (!contest.type.equals("Referendum")) {
                Log.d(TAG, "No candidates found for selected contest.");

                myActivity.findViewById(R.id.contest_candidate_list_header).setVisibility(View.GONE);
            }

        } catch (Exception ex) {
            Log.e(TAG, "Failed to get contest info!");

            ex.printStackTrace();
        }
    }

    @Override
    public void onDetach() {
        // Show ballot fragment components again when user goes back
        Log.d(TAG + ":onDetach", "Showing ballot container's view again");
        VIPTabBarActivity myActivity = (VIPTabBarActivity) getActivity();
        myActivity.setCurrentFragment(R.id.ballot_fragment);
        mContainer.getChildAt(0).setVisibility(View.VISIBLE);

        super.onDetach();
    }
}
