package edu.princeton.safe.model;

import java.util.List;

public interface DomainDetails {

    List<? extends Domain> getDomains(int typeIndex);

}
