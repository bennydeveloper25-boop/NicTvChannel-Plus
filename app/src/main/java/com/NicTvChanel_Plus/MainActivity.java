package com.NicTvChanel_Plus;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NicTvLog";
    private ExoPlayer exoPlayer;
    private PlayerView playerView;
    private DrawerLayout drawerLayout;
    private TextView txtChannelName;
    private RecyclerView recyclerView;
    private ProgressBar loadingBar;

    private List<Object> listItems = new ArrayList<>(); // Lista mixta: Categorías (String) y Canales (Channel)
    private int currentChannelIndex = 0;

    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_TIME_DELTA = 500;

    // Handler para ocultar elementos automáticamente
    private final Handler hideHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            FirebaseApp.initializeApp(this);

            // --- CONFIGURACIÓN FULLSCREEN E INMERSIVA ---
            if (getSupportActionBar() != null) getSupportActionBar().hide();
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            setContentView(R.layout.activity_main);

            playerView = findViewById(R.id.player_view);
            drawerLayout = findViewById(R.id.drawer_layout);
            txtChannelName = findViewById(R.id.txt_channel_name);
            recyclerView = findViewById(R.id.recycler_channels);
            loadingBar = findViewById(R.id.loading_speed);

            // Letra 3 veces más grande para el nombre del canal en pantalla
            txtChannelName.setTextSize(45f);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            setupPlayer();
            loadM3U();

        } catch (Exception e) {
            Log.e(TAG, "Error Crítico: " + e.getMessage());
        }
    }

    private void setupPlayer() {
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                .setAllowCrossProtocolRedirects(true);

        exoPlayer = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                .build();

        Player forwardingPlayer = new ForwardingPlayer(exoPlayer) {
            @Override public void seekToNext() { nextChannel(); }
            @Override public void seekToPrevious() { previousChannel(); }
            @Override public boolean isCommandAvailable(int command) {
                if (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) return true;
                return super.isCommandAvailable(command);
            }
        };

        playerView.setPlayer(forwardingPlayer);
        playerView.setShowFastForwardButton(false);
        playerView.setShowRewindButton(false);
        playerView.setShowNextButton(true);
        playerView.setShowPreviousButton(true);

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    loadingBar.setVisibility(View.VISIBLE);
                } else if (state == Player.STATE_READY) {
                    loadingBar.setVisibility(View.GONE);
                    // Ocultar nombre y controles tras 3 segundos de reproducción
                    startHideTimer();
                } else if (state == Player.STATE_ENDED) {
                    loadingBar.setVisibility(View.GONE);
                }
            }
            @Override
            public void onPlayerError(PlaybackException error) {
                loadingBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Señal no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        playerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override public void onSwipeLeft() { nextChannel(); }
            @Override public void onSwipeRight() { previousChannel(); }
            @Override public void onSwipeDown() { toggleDrawer(); }
            @Override public void onClick() { handleSmartClick(); }
        });
    }

    private void startHideTimer() {
        hideHandler.removeCallbacksAndMessages(null);
        hideHandler.postDelayed(() -> {
            txtChannelName.setVisibility(View.GONE);
            playerView.hideController();
        }, 3000); // 3 segundos
    }

    private void handleSmartClick() {
        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
            toggleDrawer();
        } else {
            if (playerView.isControllerFullyVisible()) {
                playerView.hideController();
                txtChannelName.setVisibility(View.GONE);
            } else {
                playerView.showController();
                txtChannelName.setVisibility(View.VISIBLE);
                startHideTimer(); // Reiniciar cronómetro al mostrar
            }
        }
        lastClickTime = clickTime;
    }

    private void loadM3U() {
        loadingBar.setVisibility(View.VISIBLE);
        String url = "https://example.com/demo.m3u8";
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String content = response.body().string();
                    String[] lines = content.split("\n");
                    List<Object> tempItems = new ArrayList<>();
                    String lastCategory = "";

                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].trim();
                        if (line.startsWith("#EXTINF")) {
                            // Extraer Categoría
                            String category = "General";
                            if (line.contains("group-title=\"")) {
                                category = line.split("group-title=\"")[1].split("\"")[0];
                            }

                            // Si la categoría cambió, añadir un separador de texto (String)
                            if (!category.equals(lastCategory)) {
                                tempItems.add(category);
                                lastCategory = category;
                            }

                            // Extraer Nombre
                            String name = line.contains(",") ? line.substring(line.lastIndexOf(",") + 1).trim() : "Canal";

                            // Extraer Link
                            for (int j = i + 1; j < lines.length; j++) {
                                String link = lines[j].trim();
                                if (!link.isEmpty() && !link.startsWith("#")) {
                                    tempItems.add(new Channel(name, link));
                                    break;
                                }
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        loadingBar.setVisibility(View.GONE);
                        listItems.clear();
                        listItems.addAll(tempItems);

                        ChannelAdapter adapter = new ChannelAdapter(listItems, channel -> {
                            playVideo(channel.getUrl(), channel.getName());
                            drawerLayout.closeDrawer(GravityCompat.START);
                        });
                        recyclerView.setAdapter(adapter);

                        // Reproducir el primer canal encontrado
                        for (Object item : listItems) {
                            if (item instanceof Channel) {
                                playVideo(((Channel) item).getUrl(), ((Channel) item).getName());
                                break;
                            }
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> loadingBar.setVisibility(View.GONE));
            }
        }).start();
    }

    public void nextChannel() {
        changeChannel(1);
    }

    public void previousChannel() {
        changeChannel(-1);
    }

    private void changeChannel(int direction) {
        if (listItems.isEmpty()) return;

        // Buscar el siguiente objeto de tipo Channel en la lista mixta
        int size = listItems.size();
        for (int i = 1; i <= size; i++) {
            int nextIndex = (currentChannelIndex + (i * direction) + size) % size;
            if (listItems.get(nextIndex) instanceof Channel) {
                currentChannelIndex = nextIndex;
                Channel next = (Channel) listItems.get(currentChannelIndex);
                playVideo(next.getUrl(), next.getName());
                break;
            }
        }
    }

    private void playVideo(String url, String name) {
        if (exoPlayer == null) return;
        loadingBar.setVisibility(View.VISIBLE);
        txtChannelName.setText(name);
        txtChannelName.setVisibility(View.VISIBLE);
        playerView.showController();

        MediaItem item = new MediaItem.Builder()
                .setUri(url)
                .setMimeType(url.contains(".m3u8") ? MimeTypes.APPLICATION_M3U8 : MimeTypes.VIDEO_UNKNOWN)
                .build();

        exoPlayer.setMediaItem(item);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void toggleDrawer() {
        if (drawerLayout != null) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
                recyclerView.requestFocus(); // Importante para control remoto
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            // Doble OK para cerrar lista, un OK para mostrar controles
            long time = System.currentTimeMillis();
            if (time - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            } else {
                handleSmartClick();
            }
            lastClickTime = time;
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) { nextChannel(); return true; }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) { previousChannel(); return true; }

        // Direccional abajo para abrir lista
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                toggleDrawer();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideHandler.removeCallbacksAndMessages(null);
        if (exoPlayer != null) exoPlayer.release();
    }
}
