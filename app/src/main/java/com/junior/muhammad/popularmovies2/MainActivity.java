package com.junior.muhammad.popularmovies2;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.junior.muhammad.popularmovies2.data.MoviesContract;
import com.junior.muhammad.popularmovies2.loaders.FavoriteMoviesLoader;
import com.junior.muhammad.popularmovies2.loaders.MovieAsyncTaskLoader;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements android.support.v4.app.LoaderManager.LoaderCallbacks<ArrayList<Movie>>,
        MoviesAdapter.OnItemClickListener {

    private static final String TAG = MainActivity.class.toString();

    Context mContext;

    @BindView(R.id.navigation)
    BottomBar bottomBar;

    //string to indicate which sorting mode must be performed it been default on most popular endPoint
    private static String howToSort = Constants.MOST_POPULAR_MOVIES;

    @BindView(R.id.rv_movies)
    RecyclerView recyclerView;

    private MoviesAdapter adapter;

    //progress bar to show while the recyclerView loading
    @BindView(R.id.pb_searching)
    ProgressBar mProgressBar;

    //list of movies to hold data returned from the loader
    private ArrayList<Movie> mListOfMovies;

    private LoaderManager.LoaderCallbacks<Cursor> favoriteLoader =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {

                    return new FavoriteMoviesLoader(getApplicationContext());

                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

                    Log.d(TAG, "onLoadFinished invoked");

                    extractFromCursor(data);
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {

                    adapter = new MoviesAdapter(null, null, null);
                }

            };

    private void extractFromCursor(Cursor data) {

        ArrayList<Movie> movies = new ArrayList<>();

        if (data == null) {
            return;
        }

        int titleColumnIndex = data.getColumnIndex(MoviesContract.FavEntry.COLUMN_TITLE);
        int ratingColumnIndex = data.getColumnIndex(MoviesContract.FavEntry.COLUMN_RATING);
        int dateColumnIndex = data.getColumnIndex(MoviesContract.FavEntry.COLUMN_RELEASE_DATE);
        int overviewColumnIndex = data.getColumnIndex(MoviesContract.FavEntry.COLUMN_OVERVIEW);
        int posterPathColumnIndex = data.getColumnIndex(MoviesContract.FavEntry.COLUMN_POSTER_PATH);
        int idColumnIndex = data.getColumnIndex(MoviesContract.FavEntry.COLUMN_MOVIE_ID);

        while (data.moveToNext()) {

            String title = data.getString(titleColumnIndex);
            String rating = data.getString(ratingColumnIndex);
            String date = data.getString(dateColumnIndex);
            String overview = data.getString(overviewColumnIndex);
            String posterPath = data.getString(posterPathColumnIndex);
            String movieId = data.getString(idColumnIndex);

            movies.add(new Movie(title, rating, date, overview, posterPath, movieId));

            Log.d(TAG, "onLoadFinished() called with: loader = [" + title + "");
        }

        //instantiating the adapter with the new data and make set it on the RecyclerView
        adapter = new MoviesAdapter(this, movies, this);
        recyclerView.setAdapter(adapter);

        //updating the list that we pass to details activity intent
        mListOfMovies = movies;
    }

    /**
     * Save the parcelable ArrayList of our object to be able to use it later in onRestore method
     *
     * @param outState is the Bundle will be saving our data
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(Constants.BUNDLE_KEY_FOR_MOVIES, mListOfMovies);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();

        //binding the butterKnife library
        ButterKnife.bind(this);

        //recyclerView setup
        recyclerView.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        //if the Bundle reference is not null get its content and set the adapter and show the data
        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.BUNDLE_KEY_FOR_MOVIES)) {
            mListOfMovies = savedInstanceState.getParcelableArrayList(Constants.BUNDLE_KEY_FOR_MOVIES);
            adapter = new MoviesAdapter(this, mListOfMovies, this);
            recyclerView.setAdapter(adapter);
            mProgressBar.setVisibility(View.GONE); // if data will be shown there's no need to progressBar
        } else {

            Log.d(TAG, "else invoked");
            //check if there internet connection and show a toast to the user if not
            initTheLoaderIfThereConnection();
        }

        settingListenerToNavigationTabs();

        getSupportLoaderManager().initLoader(Constants.FAVORITES_LOADER, null, favoriteLoader);

    }

    private void settingListenerToNavigationTabs() {
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {

                //first check if the device online then restart the loader to query the new end point
                if (isOnline()) {
                    switch (tabId) {
                        case R.id.most_popular_id:
                            //first set the howToSort String to most popular movies constant
                            howToSort = Constants.MOST_POPULAR_MOVIES;
                            //restart the loader method to fill the loader with new sort of movies
                            restartTheLoader();
                            Toast.makeText(mContext, R.string.most_popular_selected, Toast.LENGTH_SHORT).show();
                            break;

                        case R.id.top_rated_id:
                            //first set the howToSort String to top rated movies constant
                            howToSort = Constants.TOP_RATED_MOVIES;
                            //restart the loader method to fill the loader with new sort of movies
                            restartTheLoader();
                            Toast.makeText(mContext, R.string.top_rated_selected, Toast.LENGTH_SHORT).show();
                            break;

                        case R.id.favorite_movies_id:

                            getSupportLoaderManager().restartLoader(Constants.FAVORITES_LOADER, null, favoriteLoader);

                            Toast.makeText(mContext, R.string.favorite_selected, Toast.LENGTH_SHORT).show();

                            break;
                    }
                } else {//if there's no connection and the list is null show toast to tell the user that app need internet
                    Toast.makeText(mContext, R.string.internet_required, Toast.LENGTH_LONG).show();

                }
            }
        });
    }


    //Start of methods related to the loader
    private void initTheLoaderIfThereConnection() {
        if (isOnline()) {
            //init of the loader obviously :D
            getSupportLoaderManager().initLoader(Constants.MOVIES_LOADER, null, this);
        } else {
            Toast.makeText(this, R.string.internet_required, Toast.LENGTH_LONG).show();
        }
    }

    private void restartTheLoader() {
        getSupportLoaderManager().restartLoader(Constants.MOVIES_LOADER, null, this);
    }


    @Override
    public Loader<ArrayList<Movie>> onCreateLoader(int id, Bundle args) {
        return new MovieAsyncTaskLoader(this, howToSort); // change on user demand
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Movie>> loader, ArrayList<Movie> data) {

        //check if the ArrayList not null and if it's not pass it to the adapter and show the data
        if (data != null && !data.isEmpty()) {
            mProgressBar.setVisibility(View.INVISIBLE); // make progressBar invisible after the data is loaded
            adapter = new MoviesAdapter(this, data, this);
            recyclerView.setAdapter(adapter);
            mListOfMovies = data; // then pass the list of most popular movies
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Movie>> loader) {
        //reset the adapter when no longer needed
        adapter = new MoviesAdapter(null, null, null);
    }
//End of methods related to the loader

    @Override
    public void onClick(int position) {

        Movie movie = mListOfMovies.get(position);

        //instantiating an intent to start details activity passing it the parcelable Movie object
        Intent intent = new Intent(this, DetailsScreen.class);
        intent.putExtra(Constants.MOVIE_OBJECT_TAG, movie);

        startActivity(intent);
    }

    /**
     * A method based on stackOverFlow answer to check if the internet available on the targeted
     * device or not
     */
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = null;
        if (cm != null) {
            netInfo = cm.getActiveNetworkInfo();
        }
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

}

