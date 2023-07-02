package org.inksnow.asteroid.internal;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

@Slf4j
public final class AkTransferListener implements TransferListener {
  public static String humanReadableByteCountBin(long bytes) {
    long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
    if (absB < 1024) {
      return bytes + " B";
    }
    long value = absB;
    CharacterIterator ci = new StringCharacterIterator("KMGTPE");
    for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
      value >>= 10;
      ci.next();
    }
    value *= Long.signum(bytes);
    return String.format("%.1f %ciB", value / 1024.0, ci.current());
  }

  @Override
  public void transferInitiated(TransferEvent event) throws TransferCancelledException {
    logger.debug("transfer initiated: {}", event);
  }

  @Override
  public void transferStarted(TransferEvent event) throws TransferCancelledException {
    logger.debug("transfer started: {}", event);
  }

  @Override
  public void transferProgressed(TransferEvent event) throws TransferCancelledException {
    logger.trace("transfer progressed: {}", event);
  }

  @Override
  public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
    logger.trace("transfer corrupted: {}", event);
  }

  @Override
  public void transferSucceeded(TransferEvent event) {
    logger.debug("transfer succeeded: {}", event);
    logger.info("downloaded {} in {}, size={}",
        event.getResource().getResourceName(),
        event.getResource().getRepositoryId(),
        humanReadableByteCountBin(event.getTransferredBytes())
    );
  }

  @Override
  public void transferFailed(TransferEvent event) {
    logger.debug("transfer failed: {}", event);
  }
}
