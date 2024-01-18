package ru.nikidzawa.networkAPI.network.helpers;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ResponseStatus {
    private boolean status;
    private Exception exception;
}
