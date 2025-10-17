
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.lang.Language;
import chatty.util.BatchAction;
import chatty.util.hotkeys.Hotkey;
import chatty.util.tts.SpeakRequest;
import chatty.util.tts.TextToSpeech;
import chatty.util.tts.VoiceInfo;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 *
 * @author tduva
 */
public class TextToSpeechSettings extends SettingsPanel {

    private final SettingsDialog d;
    
    private final VoiceSelection voiceSelection;
    private final SliderLongSetting volumeSlider;
    private final SliderLongSetting rateSlider;
    private final SliderLongSetting pitchSlider;
    
    public TextToSpeechSettings(SettingsDialog d) {
        this.d = d;
        
        JPanel main = addTitledPanel(Language.getString("settings.section.tts"), 0, true);
        JPanel chat = addTitledPanel(Language.getString("settings.section.chat"), 1, true);
        JPanel hotkeys = addTitledPanel(Language.getString("settings.section.ttsHotkeys"), 2, true);
        
        //------
        // Main
        //------
        main.add(d.addSimpleBooleanSetting("ttsEnabled"), SettingsDialog.makeGbc(0, 0, 1, 1));
        
        voiceSelection = new VoiceSelection(d);
        GuiUtil.installEscapeCloseOperation(voiceSelection);
        d.addMapSetting("ttsVoice", voiceSelection);
        
        JButton selectVoiceButton = new JButton("Select Voice");
        selectVoiceButton.addActionListener(e -> {
            voiceSelection.showDialog();
        });
        main.add(selectVoiceButton, SettingsDialog.makeGbc(0, 10, 1, 1));
        
        volumeSlider = new SliderLongSetting(JSlider.HORIZONTAL, 0, 100, 80);
        d.addLongSetting("ttsVolume", volumeSlider);
        SettingsUtil.addLabeledComponent(main, "ttsVolume", 0, 1, 1, GridBagConstraints.EAST, volumeSlider);
        volumeSlider.addChangeListener(l -> ttsTest());
        
        rateSlider = new SliderLongSetting(JSlider.HORIZONTAL, 50, 300, 100);
        d.addLongSetting("ttsRate", rateSlider);
        SettingsUtil.addLabeledComponent(main, "ttsRate", 0, 2, 1, GridBagConstraints.EAST, rateSlider);
        rateSlider.addChangeListener(l -> ttsTest());
        
        pitchSlider = new SliderLongSetting(JSlider.HORIZONTAL, -100, 100, 0);
        d.addLongSetting("ttsPitch", pitchSlider);
        SettingsUtil.addLabeledComponent(main, "ttsPitch", 0, 3, 1, GridBagConstraints.EAST, pitchSlider);
        pitchSlider.addChangeListener(l -> ttsTest());
        
        
        //---------------
        // Chat Messages
        //---------------
        chat.add(d.addSimpleBooleanSetting("ttsReadMessages"), SettingsDialog.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        chat.add(d.addSimpleBooleanSetting("ttsIgnoreEmoteOnly"), SettingsDialog.makeGbcSub(0, 1, 1, 1, GridBagConstraints.WEST));
        
        //---------
        // Hotkeys
        //---------
        addHotkeyPanel(hotkeys, "tts.on");
        addHotkeyPanel(hotkeys, "tts.off");
        addHotkeyPanel(hotkeys, "tts.pause");
        addHotkeyPanel(hotkeys, "tts.resume");
        addHotkeyPanel(hotkeys, "tts.skipForwards");
        addHotkeyPanel(hotkeys, "tts.skipBackwards");
        addHotkeyPanel(hotkeys, "tts.clearQueue");
        
        SliderLongSetting hotkeysVolumeSlider = new SliderLongSetting(JSlider.HORIZONTAL, 0, 100, 80);
        d.addLongSetting("ttsHotkeysVolume", hotkeysVolumeSlider);
        SettingsUtil.addLabeledComponent(hotkeys, "ttsHotkeysVolume", 0, 30, 1, GridBagConstraints.EAST, hotkeysVolumeSlider);
    }
    
    int hotkeyCount;
    
    private void addHotkeyPanel(JPanel base, String actionId) {
        HotkeyPanel hotkeyPanel = d.addHotkeyPanel(actionId, Hotkey.Type.GLOBAL);
        SettingsUtil.addLabeledComponent(base, "settings.label.hotkey."+actionId, 0, hotkeyCount++, 1, GridBagConstraints.EAST, hotkeyPanel);
    }
    
    private void ttsTest() {
        if (!this.isShowing()) {
            return;
        }
        BatchAction.queue(this, 1000, true, true, () -> {
                      TextToSpeech tts = TextToSpeech.get(d.settings);
                      tts.speak("This is a test message",
                                (String) voiceSelection.getSettingValue().get(tts.getProviderId()),
                                volumeSlider.getSettingValue().intValue(),
                                rateSlider.getSettingValue().intValue(),
                                pitchSlider.getSettingValue().intValue(),
                                SpeakRequest.Mode.STOP_SAY_DIRECTLY);
                  });
    }
    
    private class VoiceSelection extends JDialog implements MapSetting {
        
        private Map data;
        
        private final JList<VoiceInfo> list;
        private final DefaultListModel<VoiceInfo> listData;
        
        public VoiceSelection(SettingsDialog d) {
            super(d);
            setModal(true);
            setTitle("Available Voices");
            listData = new DefaultListModel<>();
            list = new JList<>(listData);
            list.addListSelectionListener(l -> ttsTest());
            
            JButton okButton = new JButton(Language.getString("dialog.button.ok"));
            okButton.addActionListener(e -> setVisible(false));
            
            add(list, BorderLayout.CENTER);
            add(okButton, BorderLayout.SOUTH);
        }
        
        public void showDialog() {
            if (list.getSelectedValue() == null) {
                String providerId = TextToSpeech.get(d.settings).getProviderId();
                List<VoiceInfo> voiceInfo = TextToSpeech.get(d.settings).getAvailableVoices();
                listData.clear();
                voiceInfo.forEach(v -> listData.addElement(v));
                
                String selectedVoiceName = (String) data.get(providerId);
                list.setSelectedValue(new VoiceInfo(selectedVoiceName, null, null), true);
            }
            
            pack();
            setLocationRelativeTo(getParent());
            setVisible(true);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map getSettingValue() {
            VoiceInfo selectedVoice = list.getSelectedValue();
            if (selectedVoice != null) {
                data.put(TextToSpeech.get(d.settings).getProviderId(), selectedVoice.name);
            }
            return data;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setSettingValue(Map value) {
            data = new HashMap(value);
        }
        
    }
    
}
