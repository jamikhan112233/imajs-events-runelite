package com.imajsevents;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
	private final JTextField rsnField = new JTextField();
	private final JTextField newRsnField = new JTextField();
	private final JPasswordField changeCodeField = new JPasswordField();
	private final JButton enrollButton = new JButton("Enroll");
	private final JButton changeRsnButton = new JButton("Change RSN");
	private final JTextField specialKillBossField = new JTextField();
	private final JButton startSpecialKillButton = new JButton("Start Special Kill");
	private final JButton cancelSpecialKillButton = new JButton("Cancel Special Kill");
	private final JLabel statusLabel = new JLabel("Not enrolled");
	private final JLabel specialKillStatusLabel = new JLabel("No Special Kill active");

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
		addLabelAndField(form, gbc, "Registered RSN", rsnField);
		form.add(enrollButton, gbc);
		gbc.gridy++;

		JLabel changeTitle = new JLabel("Authorized RSN change");
		changeTitle.setBorder(BorderFactory.createEmptyBorder(14, 0, 2, 0));
		form.add(changeTitle, gbc);
		gbc.gridy++;
		addLabelAndField(form, gbc, "New RSN", newRsnField);
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

		statusLabel.setForeground(Color.LIGHT_GRAY);
		statusLabel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
		form.add(statusLabel, gbc);

		enrollButton.addActionListener(event -> enroll());
		changeRsnButton.addActionListener(event -> changeRsn());
		startSpecialKillButton.addActionListener(event -> startSpecialKill());
		cancelSpecialKillButton.addActionListener(event -> cancelSpecialKill());
		add(form, BorderLayout.NORTH);
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
		plugin.enroll(competitionField.getText(), passwordField.getPassword(), rsnField.getText(),
			profile -> {
				passwordField.setText("");
				rsnField.setText(profile.getRegisteredRsn());
				setBusy(false, "Enrolled in " + profile.getCompetitionName());
			}, error -> setBusy(false, error));
	}

	private void changeRsn()
	{
		setBusy(true, "Authorizing RSN change...");
		char[] code = changeCodeField.getPassword();
		plugin.changeRsn(newRsnField.getText(), new String(code), profile -> {
			java.util.Arrays.fill(code, '\0');
			changeCodeField.setText("");
			newRsnField.setText("");
			rsnField.setText(profile.getRegisteredRsn());
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
			rsnField.setText(profile.getRegisteredRsn());
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
