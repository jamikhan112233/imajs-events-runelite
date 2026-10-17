package com.imajsevents;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class ImajEventsPanel extends PluginPanel
{
	private final ImajEventsPlugin plugin;
	private final JTextField competitionField = new JTextField();
	private final JPasswordField passwordField = new JPasswordField();
	private final JLabel currentCharacterLabel = new JLabel("Log into the character you want to enroll");
	private final JPasswordField changeCodeField = new JPasswordField();
	private final JButton enrollButton = new JButton("Enroll");
	private final JButton changeRsnButton = new JButton("Change RSN");
	private final JTextField specialKillBossField = new JTextField();
	private final JButton startSpecialKillButton = new JButton("Start Special Kill");
	private final JButton cancelSpecialKillButton = new JButton("Cancel Special Kill");
	private final JButton valueCheckButton = new JButton("Value Check");
	private final JLabel statusLabel = new JLabel("Not enrolled");
	private final JLabel specialKillStatusLabel = new JLabel("No Special Kill active");
	private final JLabel valueCheckStatusLabel = new JLabel("No value check yet");

	ImajEventsPanel(ImajEventsPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel form = new JPanel(new GridBagLayout());
		form.setBackground(ColorScheme.DARK_GRAY_COLOR);
		form.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(3, 0, 3, 0);

		addLabelAndField(form, gbc, "Competition", competitionField);
		addLabelAndField(form, gbc, "Competition password", passwordField);
		currentCharacterLabel.setForeground(Color.LIGHT_GRAY);
		form.add(currentCharacterLabel, gbc);
		gbc.gridy++;
		form.add(enrollButton, gbc);
		gbc.gridy++;

		JLabel changeTitle = new JLabel("Authorized RSN change");
		changeTitle.setBorder(BorderFactory.createEmptyBorder(14, 0, 2, 0));
		form.add(changeTitle, gbc);
		gbc.gridy++;
		JLabel rsnChangeHelp = new JLabel("Applies the code to your current logged-in character");
		rsnChangeHelp.setForeground(Color.LIGHT_GRAY);
		form.add(rsnChangeHelp, gbc);
		gbc.gridy++;
		addLabelAndField(form, gbc, "One-time change code", changeCodeField);
		form.add(changeRsnButton, gbc);
		gbc.gridy++;

		JLabel specialKillTitle = new JLabel("Special Kill (optional)");
		specialKillTitle.setBorder(BorderFactory.createEmptyBorder(14, 0, 2, 0));
		form.add(specialKillTitle, gbc);
		gbc.gridy++;
		addLabelAndField(form, gbc, "Boss name from the KC message", specialKillBossField);
		form.add(startSpecialKillButton, gbc);
		gbc.gridy++;
		form.add(cancelSpecialKillButton, gbc);
		gbc.gridy++;
		specialKillStatusLabel.setForeground(Color.LIGHT_GRAY);
		specialKillStatusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		form.add(specialKillStatusLabel, gbc);
		gbc.gridy++;

		JLabel valueCheckTitle = new JLabel("Current gear and inventory value");
		valueCheckTitle.setBorder(BorderFactory.createEmptyBorder(14, 0, 2, 0));
		form.add(valueCheckTitle, gbc);
		gbc.gridy++;
		form.add(valueCheckButton, gbc);
		gbc.gridy++;
		valueCheckStatusLabel.setForeground(Color.LIGHT_GRAY);
		valueCheckStatusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		form.add(valueCheckStatusLabel, gbc);
		gbc.gridy++;
		JLabel valueCheckPrivacyLabel = new JLabel("Local only — nothing is sent");
		valueCheckPrivacyLabel.setForeground(Color.GRAY);
		form.add(valueCheckPrivacyLabel, gbc);
		gbc.gridy++;

		statusLabel.setForeground(Color.LIGHT_GRAY);
		statusLabel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
		form.add(statusLabel, gbc);

		enrollButton.addActionListener(event -> enroll());
		changeRsnButton.addActionListener(event -> changeRsn());
		startSpecialKillButton.addActionListener(event -> startSpecialKill());
		cancelSpecialKillButton.addActionListener(event -> cancelSpecialKill());
		valueCheckButton.addActionListener(event -> checkCurrentValue());
		add(form, BorderLayout.NORTH);
	}

	private void checkCurrentValue()
	{
		valueCheckButton.setEnabled(false);
		valueCheckStatusLabel.setText("Calculating...");
		plugin.checkCurrentValue(result -> {
			valueCheckButton.setEnabled(true);
			valueCheckStatusLabel.setText("<html>Equipment: " + formatGp(result.getEquipmentValue())
				+ " gp<br>Inventory: " + formatGp(result.getInventoryValue())
				+ " gp<br><b>Total: " + formatGp(result.getTotalValue()) + " gp</b></html>");
		}, error -> {
			valueCheckButton.setEnabled(true);
			valueCheckStatusLabel.setText(error);
		});
	}

	private static String formatGp(long value)
	{
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}

	private void startSpecialKill()
	{
		startSpecialKillButton.setEnabled(false);
		specialKillStatusLabel.setText("Capturing starting setup...");
		plugin.startSpecialKill(specialKillBossField.getText(), message -> {
			startSpecialKillButton.setEnabled(true);
			specialKillStatusLabel.setText(message);
		}, error -> {
			startSpecialKillButton.setEnabled(true);
			specialKillStatusLabel.setText(error);
		});
	}

	private void cancelSpecialKill()
	{
		plugin.cancelSpecialKill();
		startSpecialKillButton.setEnabled(true);
		specialKillStatusLabel.setText("No Special Kill active");
	}

	private void addLabelAndField(JPanel form, GridBagConstraints gbc, String label, JTextField field)
	{
		form.add(new JLabel(label), gbc);
		gbc.gridy++;
		form.add(field, gbc);
		gbc.gridy++;
	}

	private void enroll()
	{
		setBusy(true, "Enrolling...");
		plugin.enroll(competitionField.getText(), passwordField.getPassword(),
			profile -> {
				passwordField.setText("");
				currentCharacterLabel.setText("Registered character: " + profile.getRegisteredRsn());
				setBusy(false, "Enrolled in " + profile.getCompetitionName());
			}, error -> setBusy(false, error));
	}

	private void changeRsn()
	{
		setBusy(true, "Authorizing RSN change...");
		char[] code = changeCodeField.getPassword();
		plugin.changeRsn(new String(code), profile -> {
			java.util.Arrays.fill(code, '\0');
			changeCodeField.setText("");
			currentCharacterLabel.setText("Registered character: " + profile.getRegisteredRsn());
			setBusy(false, "RSN changed to " + profile.getRegisteredRsn());
		}, error -> {
			java.util.Arrays.fill(code, '\0');
			setBusy(false, error);
		});
	}

	void showProfile(CompetitionProfile profile)
	{
		SwingUtilities.invokeLater(() -> {
			competitionField.setText(profile.getCompetitionName());
			currentCharacterLabel.setText(profile.getRegisteredRsn().isEmpty()
				? "Log into the character you want to enroll"
				: "Registered character: " + profile.getRegisteredRsn());
			statusLabel.setText(profile.getCompetitionName().isEmpty()
				? "Not enrolled"
				: "Enrolled in " + profile.getCompetitionName());
		});
	}

	void showStatus(String message)
	{
		SwingUtilities.invokeLater(() -> statusLabel.setText(message));
	}

	void showSpecialKillStatus(String message)
	{
		SwingUtilities.invokeLater(() -> specialKillStatusLabel.setText(message));
	}

	private void setBusy(boolean busy, String status)
	{
		SwingUtilities.invokeLater(() -> {
			enrollButton.setEnabled(!busy);
			changeRsnButton.setEnabled(!busy);
			statusLabel.setText(status);
		});
	}
}
