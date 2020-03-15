package it.niedermann.nextcloud.deck.ui.card.attachments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.SharedElementCallback;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StableIdKeyProvider;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import it.niedermann.nextcloud.deck.DeckLog;
import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.databinding.FragmentCardEditTabAttachmentsBinding;
import it.niedermann.nextcloud.deck.model.Account;
import it.niedermann.nextcloud.deck.model.Attachment;
import it.niedermann.nextcloud.deck.persistence.sync.SyncManager;
import it.niedermann.nextcloud.deck.util.FileUtils;

import static it.niedermann.nextcloud.deck.ui.card.CardAdapter.BUNDLE_KEY_ACCOUNT_ID;
import static it.niedermann.nextcloud.deck.ui.card.CardAdapter.BUNDLE_KEY_BOARD_ID;
import static it.niedermann.nextcloud.deck.ui.card.CardAdapter.BUNDLE_KEY_CAN_EDIT;
import static it.niedermann.nextcloud.deck.ui.card.CardAdapter.BUNDLE_KEY_LOCAL_ID;
import static it.niedermann.nextcloud.deck.ui.card.CardAdapter.NO_LOCAL_ID;
import static it.niedermann.nextcloud.deck.ui.card.attachments.CardAttachmentAdapter.VIEW_TYPE_DEFAULT;
import static it.niedermann.nextcloud.deck.ui.card.attachments.CardAttachmentAdapter.VIEW_TYPE_IMAGE;

public class CardAttachmentsFragment extends Fragment implements AttachmentDeletedListener, AttachmentClickedListener {

    private FragmentCardEditTabAttachmentsBinding binding;
    private SelectionTracker<Long> selectionTracker;

    private ActionMode actionMode;

    private static final int REQUEST_CODE_ADD_ATTACHMENT = 1;
    private static final int REQUEST_PERMISSION = 2;

    private SyncManager syncManager;

    private long accountId;
    private long cardId;

    private int clickedItemPosition;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentCardEditTabAttachmentsBinding.inflate(inflater, container, false);

        Bundle args = getArguments();
        if (args != null) {
            accountId = args.getLong(BUNDLE_KEY_ACCOUNT_ID);
            cardId = args.getLong(BUNDLE_KEY_LOCAL_ID);
            if (cardId == NO_LOCAL_ID) {
                this.binding.saveCardBeforeAddAttachments.setVisibility(View.VISIBLE);
                this.binding.attachmentsList.setVisibility(View.GONE);
                this.binding.emptyContentView.setVisibility(View.GONE);
                this.binding.fab.setVisibility(View.GONE);
            } else {
                boolean canEdit = args.getBoolean(BUNDLE_KEY_CAN_EDIT);

                syncManager = new SyncManager(requireActivity());
                syncManager.getCardByLocalId(accountId, cardId).observe(getViewLifecycleOwner(), (fullCard) -> {
                    DeckLog.info("SELECTION = Updating Card");
                    if (fullCard.getAttachments().size() == 0) {
                        this.binding.emptyContentView.setVisibility(View.VISIBLE);
                        this.binding.attachmentsList.setVisibility(View.GONE);
                    } else {
                        this.binding.emptyContentView.setVisibility(View.GONE);
                        this.binding.attachmentsList.setVisibility(View.VISIBLE);
                        syncManager.readAccount(accountId).observe(getViewLifecycleOwner(), (Account account) -> {
                            DeckLog.info("SELECTION == Updating Account");
                            CardAttachmentAdapter adapter = new CardAttachmentAdapter(
                                    this,
                                    account,
                                    fullCard.getCard().getLocalId(),
                                    fullCard.getCard().getId(),
                                    fullCard.getAttachments());
                            DeckLog.info("SELECTION == Created Adapter");
                            binding.attachmentsList.setAdapter(adapter);
                            DeckLog.info("SELECTION == Setting Adapter");
                            DeckLog.info("SELECTION == selectionTracker == null: " + (selectionTracker == null));
                            if (selectionTracker == null) {
                                DeckLog.info("SELECTION == Creating SelectionTracker");
                                selectionTracker = new SelectionTracker.Builder<>(
                                        Objects.requireNonNull(CardAttachmentAdapter.class.getCanonicalName()),
                                        binding.attachmentsList,
                                        new StableIdKeyProvider(binding.attachmentsList), // new CardAttachmentKeyProvider(1, fullCard.getAttachments()),
                                        new CardAttachmentLookup(binding.attachmentsList),
                                        StorageStrategy.createLongStorage()
                                ).build();
                                if (getActivity() instanceof AppCompatActivity) {
                                    DeckLog.info("SELECTION == Adding Observer");
                                    selectionTracker.addObserver(new SelectionTracker.SelectionObserver() {
                                        @Override
                                        public void onSelectionChanged() {
                                            super.onSelectionChanged();
                                            DeckLog.info("SELECTION === onSelectionChanged()");
                                            if (selectionTracker.hasSelection() && actionMode == null) {
                                                DeckLog.info("SELECTION === Starting ActionMode");
                                                actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(
                                                        new ActionModeController(
                                                                requireContext(),
                                                                account.getUrl(),
                                                                fullCard.getId(),
                                                                fullCard.getAttachments(),
                                                                selectionTracker,
                                                                CardAttachmentsFragment.this
                                                        )
                                                );
                                            } else if (!selectionTracker.hasSelection() && actionMode != null) {
                                                DeckLog.info("SELECTION === Finishing ActionMode");
                                                actionMode.finish();
                                                actionMode = null;
                                            } else if (actionMode != null) {
                                                DeckLog.info("SELECTION === Updating Counter in ActionMode bar");
                                                actionMode.setTitle(String.valueOf(selectionTracker.getSelection().size()));
                                                actionMode.invalidate();
                                            }
                                        }
                                    });
                                }
                                DeckLog.info("SELECTION == setSelectionTracker (selectionTracker was previously null");
                                adapter.setSelectionTracker(selectionTracker);
                                if (savedInstanceState != null) {
                                    DeckLog.info("SELECTION == Restore Instance State for SelectionTracker");
                                    selectionTracker.onRestoreInstanceState(savedInstanceState);
                                }
                            } else {
                                DeckLog.info("SELECTION == setSelectionTracker (selectionTracker was previously not null)");
                                adapter.setSelectionTracker(selectionTracker);
                            }

                            // https://android-developers.googleblog.com/2018/02/continuous-shared-element-transitions.html?m=1
                            // https://github.com/android/animation-samples/blob/master/GridToPager/app/src/main/java/com/google/samples/gridtopager/fragment/ImagePagerFragment.java
                            setExitSharedElementCallback(new SharedElementCallback() {
                                @Override
                                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                                    AttachmentViewHolder selectedViewHolder = (AttachmentViewHolder) binding.attachmentsList
                                            .findViewHolderForAdapterPosition(clickedItemPosition);
                                    if (selectedViewHolder != null) {
                                        sharedElements.put(names.get(0), selectedViewHolder.getPreview());
                                    }
                                }
                            });

                            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                            int spanCount = (int) ((displayMetrics.widthPixels / displayMetrics.density) / getResources().getInteger(R.integer.max_dp_attachment_column));
                            GridLayoutManager glm = new GridLayoutManager(getActivity(), spanCount);

                            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                                @Override
                                public int getSpanSize(int position) {
                                    switch (adapter.getItemViewType(position)) {
                                        case VIEW_TYPE_IMAGE:
                                            return 1;
                                        case VIEW_TYPE_DEFAULT:
                                            return spanCount;
                                        default:
                                            return 1;
                                    }
                                }
                            });
                            binding.attachmentsList.setLayoutManager(glm);
                        });
                    }
                });
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && canEdit) {
                    binding.fab.setOnClickListener(v -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    REQUEST_PERMISSION);
                        } else {
                            DeckLog.info("SELECTION == FAB clicked -> Finish ActionMode");
                            actionMode.finish();
                            startFilePickerIntent();
                        }
                    });
                    binding.fab.show();
                    binding.attachmentsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                            if (dy > 0)
                                binding.fab.hide();
                            else if (dy < 0)
                                binding.fab.show();
                        }
                    });
                } else {
                    binding.fab.hide();
                    binding.emptyContentView.hideDescription();
                }
            }
        }


        return binding.getRoot();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void startFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_ADD_ATTACHMENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_ATTACHMENT && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    DeckLog.info("Uri: " + uri.toString());
                    String path = FileUtils.getPath(getContext(), uri);
                    if (path != null) {
                        File uploadFile = new File(path);
                        if (cardId == NO_LOCAL_ID) {
                            if (getActivity() instanceof AttachmentAddedToNewCardListener) {
                                Toast.makeText(getContext(), "You need to save the card first.", Toast.LENGTH_LONG).show();
//                                Attachment attachment = new Attachment();
//                                ((AttachmentAddedToNewCardListener) getActivity()).attachmentAddedToNewCard(attachment);
                            }
                        } else {
                            syncManager.addAttachmentToCard(accountId, cardId, Attachment.getMimetypeForUri(getContext(), uri), uploadFile);
                        }
                    } else {
                        DeckLog.warn("path to file is null");
                    }
                } else {
                    DeckLog.warn("data.getDate() returned null");
                }
            } else {
                DeckLog.warn("data is null");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                startFilePickerIntent();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        selectionTracker.onSaveInstanceState(outState);
    }

    public CardAttachmentsFragment() {
    }

    public static CardAttachmentsFragment newInstance(long accountId, long localId, long boardId, boolean canEdit) {
        Bundle bundle = new Bundle();
        bundle.putLong(BUNDLE_KEY_ACCOUNT_ID, accountId);
        bundle.putLong(BUNDLE_KEY_BOARD_ID, boardId);
        bundle.putLong(BUNDLE_KEY_LOCAL_ID, localId);
        bundle.putBoolean(BUNDLE_KEY_CAN_EDIT, canEdit);

        CardAttachmentsFragment fragment = new CardAttachmentsFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onAttachmentDeleted(long attachmentLocalId) {
        syncManager.deleteAttachmentOfCard(accountId, cardId, attachmentLocalId);
    }

    @Override
    public void onAttachmentClicked(int position) {
        this.clickedItemPosition = position;
    }

    public interface AttachmentAddedToNewCardListener {
        void attachmentAddedToNewCard(Attachment attachment);
    }
}
