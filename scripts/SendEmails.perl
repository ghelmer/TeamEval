#!/usr/bin/perl

use Data::Dumper;
use MIME::Lite;
use Spreadsheet::WriteExcel;
use strict;

my $mailhost = 'mailin.iastate.edu';

my $course = 'MIS307 Fall 2015';
my $due_date = 'Friday, Dec 11 11:59PM';
my $prepared_dir = '/Users/ghelmer/Documents/Classes/MIS307F15/Homeworks/Team Evaluations/Prepared';

my $student_team_fn;
if (@ARGV > 0) {
    $student_team_fn = shift(@ARGV);
}
if (length($student_team_fn) == 0) {
    $student_team_fn = '/Users/ghelmer/Documents/Classes/MIS307F15/Homeworks/Team Evaluations/students-teams.txt';
    print STDERR "Using default input file ${student_team_fn}\n";
}

my %team_by_id;
my %fullname_by_id;
my %filename_by_id;
my %team_members_fullnames;
my %teams;
my %recip;

open(IN, "< $student_team_fn") || die "Could not open ${student_team_fn}: $!";
while (<IN>) {
    chomp;
    next if /^#/;
    my ($lastname, $firstname, $id, $team) = split(/\t/);
    my $fullname = $lastname . ', ' . $firstname;
    $team_by_id{$id} = $team;
    $fullname_by_id{$id} = $fullname;
    if (!defined($teams{$team})) {
		$teams{$team} = [];
    }
    push(@{$teams{$team}}, $id);
    if (!defined($team_members_fullnames{$team})) {
    	$team_members_fullnames{$team} = [];
	}
	push(@{$team_members_fullnames{$team}}, $fullname);
    $recip{$id} = 0;
}
close(IN);

foreach my $key (sort {$a <=> $b} keys(%teams)) {
    print "Team $key: ", Dumper($teams{$key}), "\n";
}

foreach my $team (keys(%teams)) {
	foreach my $id (@{$teams{$team}}) {
	    $filename_by_id{$id} = buildSpreadsheetForTeamMember($team, $id, $team_members_fullnames{$team});
	}
}

MIME::Lite->send('smtp', $mailhost, Timeout=>60);

my $done = 0;
while (!$done) {
    my $retry = 0;
    foreach my $key (sort {$a <=> $b} keys(%teams)) {
	foreach my $i (@{$teams{$key}}) {
	    next if $recip{$i};
	    if (!defined($filename_by_id{$i})) {
		print STDERR "No file for user ID ${i}!\n";
		next;
	    }
	    my $msg = MIME::Lite->new(
		From    => "ghelmer\@iastate.edu",
		To      => "${i}\@iastate.edu",
		CC      => "ghelmer\@iastate.edu",
		Subject => "${course} Final Project Team Evaluation for Team ${key} Member ${i}",
		Type    =>'multipart/related'
		);
	    $msg->attach(
		Type => 'text/html',
		Data => "<body>\n" .
		"Please find attached the Excel spreadsheet for ${course} Team ${key}.\n" .
		"Please fill in your evaluation for each team member (including yourself)\n" .
		"and email it to <a href=\"mailto:ghelmer\@iastate.edu\">ghelmer\@iastate.edu</a> by\n" .
		"<strong>${due_date}.</strong>\n" .
		"<p/>\n" .
		"<strong>Please do not change the name of the spreadsheet file</strong>.\n" .
		"If you edit it in Google Docs, please download the spreadsheet before\n" .
		"mailing it.\n" .
		"<p/></body>\n",
		);
	    $msg->attach(
		Type => 'application/vnd.ms-excel',
		Id   => "${key}-${i}.xls",
		Filename=> "${key}-${i}.xls",
		Path => $filename_by_id{$i},
		);
	    my $sent = 0;
	    # Catch unexpected termination during send.
	    eval { $msg->send; };
	    if ($@) {
		print "Sending message to ${i} failed: $@\n";
	    } else {
		$sent = $msg->last_send_successful();
	    }
	    if (!$sent) {
		print "Sending message to ${i} was unsuccessful.\n";
		$retry++;
	    } else {
		print "Sent message to ${i}.\n";
		$recip{$i} = 1;
	    }
	}
    }
    if ($retry == 0) {
	$done = 1;
    } else {
	print "${retry} receipients left. Waiting one minute.\n";
	sleep(60);
    }
}

exit 0;

sub buildSpreadsheetForTeamMember {
    my ($teamName, $id, $teamMemberNamesRef) = @_;
    my $row;
    my $col;
    my $fn = "${prepared_dir}/${teamName}-${id}.xls";

    # Create a new Excel workbook
    my $workbook = Spreadsheet::WriteExcel->new($fn);

    # Add a worksheet
    my $worksheet = $workbook->add_worksheet();

    $worksheet->set_column('A:A', 20);
    $worksheet->set_column('B:F', 25);
    $worksheet->set_column('G:G', 15);
    $worksheet->set_row(1, 40);
    $worksheet->set_selection('B5');

    #  Add and define the heading format
    my $heading_format = $workbook->add_format(bold => 1, size => 18, align => 'center');
    my $header_format = $workbook->add_format(bold => 1, align => 'center', valign => 'top', text_wrap => 1);
    
    # Heading
    $worksheet->merge_range('A1:G1', "${course} Final Project Peer Evaluation", $heading_format);
    #$worksheet->write('A1', "", $format);

    # Instructions
    my $instructions_format = $workbook->add_format(text_wrap => 1, valign => 'top', align => 'left');
    $worksheet->merge_range('A2:F2', 'This self and peer evaluation asks about how you and each of your ' .
			    'teammates contributed to the team during the time period you are evaluating. For each way of '.
			    'contributing, please read the behaviors that describe a "1", "3", and "5" rating. Then ' .
			    'confidentially rate yourself and your teammates. ' .
			    'NOTE: Scores of all-5\'s for any team member will NOT be accepted unless ' .
			    'sufficient justification is given at the bottom of the spreadsheet for ' .
			    'such extraordinary effort.', $instructions_format);								
    
    $worksheet->write('A3', $id);
    my $categories_format = $workbook->add_format(bold => 1, align => 'center', text_wrap => 1);
    $worksheet->merge_range('B3:F3', 'Categories', $categories_format);

    $worksheet->write('A4', 'Team Members', $header_format);
    $worksheet->write('B4', 'Contributing to the Team\'s Work', $header_format);
    $worksheet->write('C4', 'Interacting with Teammates', $header_format);
    $worksheet->write('D4', 'Keeping the Team on Track', $header_format);
    $worksheet->write('E4', 'Expecting Quality', $header_format);
    $worksheet->write('F4', 'Having Relevant Knowledge, Skills, and Abilities', $header_format);
    $worksheet->write('G4', 'Total', $header_format);
    
    $row = 4;
    for (my $teamMemberIdx = 0; $teamMemberIdx < @{$teamMemberNamesRef}; $teamMemberIdx++, $row++) {
	$worksheet->write($row, 0, $teamMemberNamesRef->[$teamMemberIdx]);
	my $sum = sprintf('=SUM(B%d:F%d)', $row + 1, $row + 1);
	$worksheet->write($row, 6, $sum);
    }
    $row++;
    
    $worksheet->write($row, 0, 'Scoring Guide', $header_format);
    $row++;

    my $instructions_format = $workbook->add_format(text_wrap => 1, valign => 'top', align => 'left');
    $worksheet->write($row, 0, '5 Points', $header_format);
    $worksheet->write($row, 1, "* Does more or higher-quality work than expected.\n" .
		      "* Makes important contributions that improve the team's work.\n" .
		      "* Helps to complete the work of teammates who are having difficulty", $instructions_format);
    $worksheet->write($row, 2, "* Asks for and shows interest in teammates' ideas and contributions.\n" .
		      "* Improves communication among teammates. Provides encouragement or enthusiasm to the team.\n" .
		      "* Asks teammates for feedback and uses their suggestions to improve.", $instructions_format);
    $worksheet->write($row, 3, "* Watches conditions affecting the team and monitors the team's progress.\n" .
		      "* Makes sure that teammates are making appropriate progress.\n" .
		      "* Gives teammates specific, timely, and constructive feedback.", $instructions_format);
    $worksheet->write($row, 4, "* Motivates the team to do excellent work.\n" .
		      "* Cares that the team does outstanding work, even if there is no additional reward.\n" .
		      "* Believes that the team can do excellent work.", $instructions_format);
    $worksheet->write($row, 5, "* Demonstrates the knowledge, skills, and abilities to do excellent work.\n" .
		      "* Acquires new knowledge or skills to improve the team's performance.\n" .
		      "* Able to perform the role of any team member if necessary.", $instructions_format);
    $row++;

    $worksheet->write($row, 0, "3 Points", $header_format);
    $worksheet->write($row, 1, "* Completes a fair share of the team's work with acceptable quality.\n" .
		      "* Keeps commitments and completes assignments on time.\n" .
		      "* Fills in for teammates when it is easy or important.", $instructions_format);
    $worksheet->write($row, 2, "* Listens to teammates and respects their contributions.\n" .
		      "* Communicates clearly. Shares information with teammates.\n" .
		      "* Participates fully in team activities.\n" .
		      "* Respects and responds to feedback from teammates.", $instructions_format);
    $worksheet->write($row, 3, "* Notices changes that influence the team's success.\n" .
		      "* Knows what everyone on the team should be doing and notices problems.\n" .
		      "* Alerts teammates of suggests solutions when the team's success is threatened.", $instructions_format);
    $worksheet->write($row, 4, "* Encourages the team to do good work that meets all requirements.\n" .
		      "* Wants the team to perform well enough to earn all available rewards.\n" .
		      "* Believes that the team can fully meets its responsibilities.", $instructions_format);
    $worksheet->write($row, 5, "* Has sufficient knowledge, skills, and abilities to contribute to the team's work.\n" .
		      "* Acquires knowledge or skills needed to meet requirements.\n" .
		      "* Able to perform some of the tasks normally done by other team members.", $instructions_format);
    $row++;

    $worksheet->write($row, 0, "1 Point", $header_format);
    $worksheet->write($row, 1, "* Does not do a fair share of the team's work.\n" .
		      "Delivers sloppy or incomplete work.\n" .
		      "* Misses deadlines. Is late, unprepared, or absent for team meetings.\n" .
		      "* Does not assist teammates. Quits if the work becomes difficult.", $instructions_format);
    $worksheet->write($row, 2, "* Interrupts, ignores, bosses, or makes fun of teammates.\n" .
		      "* Takes actions that affect teammates without their input. Does not share information.\n" .
		      "* Complains, makes excuses, or does not interact with teammates. Accepts no help or advice.", $instructions_format);
    $worksheet->write($row, 3, "* Is unaware of whether the team is meeting its goals.\n" .
		      "* Does not pay attention to teammates' progress.\n" .
		      "* Avoids discussing team problems, even when they are obvious.", $instructions_format);
    $worksheet->write($row, 4, "* Satisfied even if the team does not meet assigned standards.\n" .
		      "* Wants the team to avoid work, even if it hurts the team.\n" .
		      "* Doubts that the team can meets its requirements.", $instructions_format);
    $worksheet->write($row, 5, "* Missing basic qualifications needed to be a member of the team.\n" .
		      "* Unable or unwilling to develop knowledge or skills to contribute to the team.\n" .
		      "* Unable to perform any of the duties of the other team members.", $instructions_format);
    $row++;
    
    $worksheet->write($row, 0, "Justification for all-5's scores:", $instructions_format);
    my $just_format = $workbook->add_format(align => 'left', valign => 'top');
    $worksheet->merge_range($row, 1, $row, 5, '', $just_format);
    $row++;

    $workbook->close();
    return $fn;
}
