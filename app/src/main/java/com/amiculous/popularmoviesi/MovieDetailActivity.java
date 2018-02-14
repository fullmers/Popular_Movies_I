package com.amiculous.popularmoviesi;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.amiculous.popularmoviesi.adapters.VideoAdapter;
import com.amiculous.popularmoviesi.data.FavoriteMoviesContract.FavoritesEntry;
import com.amiculous.popularmoviesi.data.Movie;
import com.amiculous.popularmoviesi.data.MovieExtras;
import com.amiculous.popularmoviesi.data.MovieReview;
import com.amiculous.popularmoviesi.data.MovieVideo;
import com.amiculous.popularmoviesi.loaders.MovieExtrasLoader;
import com.amiculous.popularmoviesi.utils.NetworkUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MovieDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<MovieExtras>,
        VideoAdapter.VideoClickListener {

    private Movie mMovie;
    private int mScreenWidth;
    private Uri mUri;
    private MovieExtras mMovieExtras;
    private MovieExtrasLoader mMovieExtrasLoader;
    private int movieId;
    private static final String TAG = MovieDetailActivity.class.getSimpleName();
    private VideoAdapter mVideoAdapter;

    @BindView(R.id.text_movie_title) TextView TvMovieTitle;
    @BindView(R.id.text_release_date) TextView TvReleaseDate;
    @BindView(R.id.text_user_rating) TextView TvUserRating;
    @BindView(R.id.text_overview) TextView TvOverview;
    @BindView(R.id.text_no_internet) TextView TvNoInternet;
    @BindView(R.id.image_movie_poster) ImageView ImageMoviePoster;
    @BindView(R.id.constraint_layout_has_internet) ConstraintLayout ClHasInternet;
    @BindView(R.id.chbx_favorite) CheckBox CbFavorite;
    @BindView(R.id.rvTrailers) RecyclerView RvVideos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);
        ButterKnife.bind(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mMovie = extras.getParcelable(getString(R.string.movie_extra_key));
            mScreenWidth = extras.getInt(getString(R.string.screen_width_extra_key));
            mUri = getIntent().getData();
            movieId = mMovie.getId();
            mMovieExtras = new MovieExtras(movieId);
            setupUI();
        }
    }

    private void setupUI() {
        TvMovieTitle.setText(mMovie.getTitle());
        TvReleaseDate.setText(getReleaseYear());
        TvUserRating.setText(Double.toString(mMovie.getVoteAverage()));
        TvOverview.setText(mMovie.getOverview());

        if (NetworkUtils.isConnectedToInternet(getApplicationContext())) {
            TvNoInternet.setVisibility(View.GONE);
            ClHasInternet.setVisibility(View.VISIBLE);
            String posterUrl = NetworkUtils.buildMoviePosterUrl(mMovie.getPosterPath(),mScreenWidth);
            Picasso.with(this)
                    .load(posterUrl)
                    .into(ImageMoviePoster);

            getSupportLoaderManager().initLoader(0, null, MovieDetailActivity.this).forceLoad();

        } else {
            TvNoInternet.setVisibility(View.VISIBLE);
            ClHasInternet.setVisibility(View.INVISIBLE);
        }

        if (isFavorite()) {
            CbFavorite.setChecked(true);
        } else {
            CbFavorite.setChecked(false);
        }

        CbFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = ((CheckBox) v).isChecked();
                if (checked) {
                    insertInFavoriteMovies();
                } else {
                    deleteFromFavoriteMovies();
                }
            }
        });
    }

    public boolean isFavorite() {
        boolean isFavorite = false;
        Cursor cursor = getContentResolver().query(
                mUri,
                null,
                null,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
                isFavorite = true;
                cursor.close();
        }

        return isFavorite;
    }

    public void insertInFavoriteMovies() {
        ContentValues contentValues= new ContentValues();
        contentValues.put(FavoritesEntry.COLUMN_MOVIE_ID, mMovie.getId());
        contentValues.put(FavoritesEntry.COLUMN_MOVIE_TITLE, mMovie.getTitle());
        contentValues.put(FavoritesEntry.COLUMN_MOVIE_POSTER_URI, mMovie.getPosterPath());
        contentValues.put(FavoritesEntry.COLUMN_MOVIE_OVERVIEW, mMovie.getReleaseDate());
        contentValues.put(FavoritesEntry.COLUMN_MOVIE_VOTE_AVERAGE, mMovie.getVoteAverage());
        contentValues.put(FavoritesEntry.COLUMN_MOVIE_RELEASE_DATE, mMovie.getReleaseDate());

        getContentResolver().insert(FavoritesEntry.CONTENT_URI, contentValues);
    }

    public void deleteFromFavoriteMovies() {
        getContentResolver().delete(mUri, FavoritesEntry.COLUMN_MOVIE_ID + "=" + mMovie.getId(), null);
    }

    private String getReleaseYear() {
        String fullDateString = mMovie.getReleaseDate();
        String[] parts = fullDateString.split("-");
        return parts[0];
    }

    @Override
    public Loader<MovieExtras> onCreateLoader(int id, Bundle args) {
        mMovieExtrasLoader = new MovieExtrasLoader(this, movieId);
        return mMovieExtrasLoader;
    }

    @Override
    public void onLoadFinished(Loader<MovieExtras> loader, MovieExtras movieExtras) {
        ArrayList<MovieVideo> videos = movieExtras.getYoutubeVideos();
        for (MovieVideo video : videos) {
            Log.d(TAG,video.getYoutubeURL().toString());
            Log.d(TAG,video.getName().toString());
        }

        ArrayList<MovieReview> reviews = movieExtras.getReviews();
        for (MovieReview review : reviews) {
            Log.d(TAG,review.getAuthor().toString());
            Log.d(TAG,review.getContent().toString());
        }

        LinearLayoutManager videoLayoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        RvVideos.setLayoutManager(videoLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                RvVideos.getContext(),
                videoLayoutManager.getOrientation());
        RvVideos.addItemDecoration(dividerItemDecoration);
        mVideoAdapter = new VideoAdapter(this, this, videos);
        RvVideos.setAdapter(mVideoAdapter);
        RvVideos.setNestedScrollingEnabled(false);

    }

    @Override
    public void onLoaderReset(Loader<MovieExtras> loader) {

    }

    @Override
    public void onMovieClick(MovieVideo video) {
        Log.d(TAG,"clicked " + video.getName());
        Log.d(TAG,"should point to " + video.getYoutubeURL().toString());
    }


    public enum MovieExtraTypes {REVIEWS, VIDEOS}
}
