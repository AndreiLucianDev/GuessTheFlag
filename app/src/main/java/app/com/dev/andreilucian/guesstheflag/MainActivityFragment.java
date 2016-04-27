package app.com.dev.andreilucian.guesstheflag;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String LOG_TAG = MainActivityFragment.class.getSimpleName();
    private static final int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList;          //image file names
    private List<String> quizCountriesList;     //countries current quiz
    private Set<String> regionSet;              //world region current quiz
    private String correctAnswer;               //correct country for the current flag
    private int totalGuesses;                   //number of guesses made
    private int correctAnswers;                 //number of correct guesses
    private int guessRows;                      //number of rows diplaying buttons
    private SecureRandom random;                //used to randomize the quiz
    private Handler handler;                    //used to delay loading next flag
    private Animation shakeAnimation;           //animation for incorrect choice

    private LinearLayout quizlinearLayout;          //layout that contains the quiz
    private TextView questionNumberTextview;        //shows current question #
    private ImageView flagImageView;                //display a flag
    private LinearLayout[] guessLinearLayouts;      //rows of answer buttons
    private TextView answerTextView;                //display correct answer

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        //load the shake animation for the incorrect answer
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        //get references for GUI component
        quizlinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextview = (TextView) view.findViewById(R.id.question_number_textViewtextView);
        flagImageView = (ImageView) view.findViewById(R.id.flag_imageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout)view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout)view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout)view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout)view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answer_textView);

        //configure listeners for the guess buttons
        for (LinearLayout row : guessLinearLayouts){
            for (int column = 0; column < row.getChildCount(); column++){
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        //set questionNumberTextview text
        questionNumberTextview.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));

        return view;
    }

    // update guessRows based on value in SharedPreferences
    public void updateGuessRows(SharedPreferences defaultSharedPreferences) {
        // get the number of guess buttons that should be displayed
        String choices = defaultSharedPreferences.getString(MainActivity.CHOICES, null);

        Log.d(LOG_TAG, "String choices : " + choices);

        //we have two buttons in one row
        guessRows = Integer.parseInt(choices) / 2;

        //hide all linearLayouts with buttons
        for(LinearLayout row : guessLinearLayouts){
            row.setVisibility(View.GONE);
        }

        //display the right number of rows
        for(int row = 0; row < guessRows; row++){
            guessLinearLayouts[row].setVisibility(View.VISIBLE);

        }
    }

    // update world regions for quiz based on values in SharedPreferences
    public void updateRegions(SharedPreferences defaultSharedPreferences) {
        regionSet = defaultSharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    // set up and start the next quiz
    public void resetQuiz() {
        // use AssetManager to get image file names for enabled regions
        AssetManager assetManager = getActivity().getAssets();
        fileNameList.clear();

        try{
            //loop through each region
            for (String region : regionSet){
                String[] paths = assetManager.list(region);

                for (String path : paths){
                    fileNameList.add(path.replace(".png", ""));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "Error loading image file names");
        }

        correctAnswers = 0;         //reset correct answers
        totalGuesses = 0;           //reset total guesses
        quizCountriesList.clear();  //clear country list

        int flagCounter  = 1;
        int numberOfFlags = fileNameList.size();

        //random selection for the quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ){
            int randomIndex = random.nextInt(numberOfFlags);

            //get the random filename
            String filename = fileNameList.get(randomIndex);

            if (!quizCountriesList.contains(filename)){
                quizCountriesList.add(filename);
                flagCounter++;
            }
        }

        //start the quiz by loading the first flag
        loadNextFlag();
    }

    // after the user guesses a correct flag, load the next flag
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void loadNextFlag() {
        // get file name of the next flag and remove it from the list
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;
        answerTextView.setText("");

        //display current question number
        questionNumberTextview.setText(getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

        //extract the region from the next image flag
        String region = nextImage.substring(0, nextImage.indexOf("-"));

        //use AssetManager to load next image form assets folder
        AssetManager assetManager = getActivity().getAssets();

        //get an InputStream to the assets
        try (InputStream inputStream = assetManager.open(region + "/" + nextImage + ".png")){
            //load the asset as a drawable and display on the flag_imageView
            Drawable flag = Drawable.createFromStream(inputStream, nextImage);
            flagImageView.setImageDrawable(flag);

            //animate the flag on to the screen
            animate(false);
        }catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Error loading image" + nextImage, e);
        }

        //suffle filenames
        Collections.shuffle(fileNameList);

        //put the correct answer at the end of fileNameList
        //use it later for verify the user answer
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        //add 2, 4, 6 or 8 guess button  based on the value of the guessRows
        for (int row = 0; row < guessRows; row++){
            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++){
                //enable the button
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                //get country name and set it as newGuessButton text
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        //randomly replace one button with the correct answer
        int row = random.nextInt(guessRows);            //pick random row
        int column = random.nextInt(2);                 //pick random column

        LinearLayout randomRow = guessLinearLayouts[row];
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);

    }

    //animates the entire quizlinearLayout on or off the screen
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animate(boolean animateOut) {
        //prevent animation in to the UI for the first question
        if (correctAnswers == 0){
            return;
        }

        //calculate center x and y
        int centerX = (quizlinearLayout.getLeft() + quizlinearLayout.getRight()) / 2;
        int centerY = (quizlinearLayout.getTop() + quizlinearLayout.getBottom()) / 2;

        //calculate animation radius
        int radius = Math.max(quizlinearLayout.getWidth(), quizlinearLayout.getHeight());

        Animator animator;

        //animate out
        if (animateOut){
            //create circular reveal animation
            animator = ViewAnimationUtils.createCircularReveal(quizlinearLayout, centerX, centerY, radius, 0);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loadNextFlag();
                }
            });
        }else{              //animate in
            animator = ViewAnimationUtils.createCircularReveal(quizlinearLayout, centerX, centerY, 0, radius);
        }
        animator.setDuration(500);
        animator.start();
    }

    //return the country name
    private String getCountryName(String filename) {
        return filename.substring(filename.indexOf("-") + 1).replace("_", " ");
    }

    // called when a guess Button is touched
    private View.OnClickListener guessButtonListener = new View.OnClickListener() {

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onClick(View v) {
            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses;

            if (guess.equals(answer)){
                ++correctAnswers;

                //display correct answer witn the green color
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer));

                disableButtons();

                if (correctAnswers >= FLAGS_IN_QUIZ){
                    //show dialog
                    DialogFragment dialogFragment = new DialogFragment(){
                        @NonNull
                        @Override
                        public Dialog onCreateDialog(Bundle savedInstanceState) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(getString(R.string.results, totalGuesses,(1000 / (double) totalGuesses)));

                            //reset quiz button
                            builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    resetQuiz();
                                }
                            });
                            return builder.create();  //return alertDialog
                        }
                    };
                    //use FragmentManager to display the dialog
                    dialogFragment.setCancelable(false);
                    dialogFragment.show(getFragmentManager(), "quiz results");
                }
                else{
                    //answer is corect but quiz not over
                    //load the next flag after 2 seconds
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animate(true);
                        }
                    }, 2000);
                }
            }else{
                //incorect answer
                flagImageView.startAnimation(shakeAnimation);

                //display text in red
                answerTextView.setText(getResources().getString(R.string.incorrect_answer) + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
                guessButton.setEnabled(false);      //disable button
            }
        }
    };

    //disable all answer buttons
    private void disableButtons() {
        for (int row = 0; row < guessRows; row++){
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++){
                guessRow.getChildAt(i).setEnabled(false);
            }
        }
    }
}
























