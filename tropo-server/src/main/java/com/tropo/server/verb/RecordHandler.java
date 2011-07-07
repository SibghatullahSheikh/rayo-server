package com.tropo.server.verb;

import java.io.File;
import java.io.IOException;

import com.tropo.core.verb.Output;
import com.tropo.core.verb.Record;
import com.tropo.core.verb.RecordCompleteEvent;
import com.tropo.core.verb.RecordCompleteEvent.Reason;
import com.tropo.core.verb.RecordPauseCommand;
import com.tropo.core.verb.RecordResumeCommand;
import com.tropo.core.verb.Ssml;
import com.tropo.core.verb.VerbCommand;
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.core.verb.VerbCompleteReason;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.record.RecordCommand;

public class RecordHandler extends AbstractLocalVerbHandler<Record, Participant> {

	private static final Loggerf log = Loggerf.getLogger(RecordHandler.class);

	private Recording recording;
	
	@Override
	public void start() {

		if (model.getTo() == null) {
			try {
				File temp = File.createTempFile("ozone", ".mp3");
				model.setTo(temp.toURI());
			} catch (IOException e) {
				log.error(e.getMessage(),e);
			}
		}
		
		RecordCommand command = new RecordCommand(model.getTo());
        if (model.getPrompt() != null) {
            Ssml prompt = model.getPrompt();
            AudibleResource audibleResource = resolveAudio(prompt);
        	OutputCommand output = new OutputCommand(audibleResource);
        	output.setBargein(model.isBargein());
        	output.setVoiceName(model.getVoice());
        	command.setPrompt(output);
        }
        if (model.getAppend() != null) {
        	command.setAppend(model.getAppend());
        }
        if (model.getDtmfTruncate() != null) {
        	command.setSignalTruncationOn(model.getDtmfTruncate());
        }
        if (model.getSilenceTerminate() != null) {
        	command.setSilenceTerminationOn(model.getSilenceTerminate());
        }
        if (model.getStartBeep() != null) {
        	command.setStartBeep(model.getStartBeep());
        }/* else {
        	command.setStartBeep(Boolean.FALSE);
        }*/
        if (model.getStartPauseMode() != null) {
        	command.setStartInPausedMode(model.getStartPauseMode());
        }
        if (model.getCodec() != null) {
        	command.setAudioCODEC(Output.toCodecValue(model.getCodec()));
        }
        if (model.getCodecParameters() != null) {
        	command.setAudioFMTP(model.getCodecParameters());
        }
        if (model.getFinalTimeout() != null) {
        	command.setFinalTimeout(model.getFinalTimeout());
        }
        if (model.getFormat() != null) {
        	command.setFileFormat(Output.toFileFormat(model.getFormat()));
        }
        if (model.getInitialTimeout() != null) {
        	command.setInitialTimeout(model.getInitialTimeout());
        }
        if (model.getMaxDuration() != null) {
        	command.setMaxDuration(model.getMaxDuration());
        }
        if (model.getMinDuration() != null) {
        	command.setMinDuration(model.getMinDuration());
        }
        if (model.getSampleRate() != null) {
        	command.setAudioClockRate(model.getSampleRate());
        } 
        
		recording = media.record(command);
	}

	@Override
	public void stop(boolean hangup) {

        recording.stop();
        if(hangup) {
            complete(new RecordCompleteEvent(model, VerbCompleteEvent.Reason.HANGUP));
        } else {
        	complete(new RecordCompleteEvent(model, Reason.SUCCESS));
        }
        
        //TODO: Complete recording
	}
	
    @Override
    public void onCommand(VerbCommand command) {
    	
        if (command instanceof RecordPauseCommand) {
        	pause();
        } else if (command instanceof RecordResumeCommand) {
        	resume();
        }
    }
	
    private void pause() {
    	
    	recording.pause();
	}
    
    private void resume() {
	
    	recording.resume();
    }
    
	@State
	public synchronized void onRecordComplete(com.voxeo.moho.event.RecordCompleteEvent event) {
		
		switch(event.getCause()) {
			case ERROR:
				log.error("Error while recording conversation");
				complete(VerbCompleteEvent.Reason.ERROR);
				break;
			case DISCONNECT:
				complete(VerbCompleteEvent.Reason.HANGUP);
				break;
		}
	}
	
	private void complete(VerbCompleteReason reason) {

		RecordCompleteEvent event = new RecordCompleteEvent(model, reason);
		complete(event);
	}
}
