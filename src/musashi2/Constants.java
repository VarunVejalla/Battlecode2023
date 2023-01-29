package musashi2;

public class Constants {

    public Constants(){
        // restrict instantiation
    }

    public static final int FULL_MASK = 65535; // 11111111111111

    //HQ Stuff
    public static final int HQ0_LOC_IDX = 0;
    public static final int HQ1_LOC_IDX = 1;
    public static final int HQ2_LOC_IDX = 2;
    public static final int HQ3_LOC_IDX = 3;
    public static final int[] HQ_LOC_IDX_MAP = {HQ0_LOC_IDX, HQ1_LOC_IDX, HQ2_LOC_IDX, HQ3_LOC_IDX};

    public static final int HQ_X_MASK = 4032;
    public static final int HQ_X_SHIFT = 6;
    public static final int HQ_Y_MASK = 63;
    public static final int HQ_Y_SHIFT = 0;

    public static final int HQ0_RESOURCES_IDX = 4;
    public static final int HQ1_RESOURCES_IDX = 5;
    public static final int HQ2_RESOURCES_IDX = 6;
    public static final int HQ3_RESOURCES_IDX = 7;
    public static final int[] HQ_RESOURCES_IDX_MAP = {HQ0_RESOURCES_IDX, HQ1_RESOURCES_IDX, HQ2_RESOURCES_IDX, HQ3_RESOURCES_IDX};

    public static final int HQ_MANA_MASK = 127; // 0000000001111111 (digits 0-6)
    public static final int HQ_MANA_SHIFT = 0;
    public static final int HQ_ADAMANTIUM_MASK = 16256; // 0011111110000000 (digits 7-13)
    public static final int HQ_ADAMANTIUM_SHIFT = 7;

    // Wells consts

    public static final int WELLS_START_IDX = 43;
    public static final int NUM_WELLS_PER_HQ = 3;
    // NOTE: IF THESE 3 VALUES EVER CHANGE, THEN MAKE SURE TO CONSIDER THAT CHANGE Robot.java readWellLocations
    // Ask srikar if you have any questions abt that
    public static final int ADAMANTIUM_WELL_OFFSET = 0;
    public static final int MANA_WELL_OFFSET = 1;
    public static final int ELIXIR_WELL_OFFSET = 2;
    public static  final int WELLS_X_MASK = 4032;
    public static final int WELLS_X_SHIFT = 6;
    public static final int WELLS_Y_MASK = 63;
    public static final int WELLS_Y_SHIFT = 0;

    // resource ratios (same indices as wells)
    public static final int RESOURCE_RATIO_MASK = 61440;
    public static final int RESOURCE_RATIO_SHIFT = 12;

    // Island consts
    public static final int ISLAND_START_IDX = 8;
    public static final int ISLAND_X_MASK = 4032;
    public static final int ISLAND_X_SHIFT = 6;
    public static final int ISLAND_Y_MASK = 63;
    public static final int ISLAND_Y_SHIFT = 0;
    public static final int ISLAND_CONTROL_MASK = 12288;
    public static final int ISLAND_CONTROL_SHIFT = 12;

    // Ratio data indices
    public static final int ADAMANTIUM_RATIO_INDEX = 0;
    public static final int MANA_RATIO_INDEX = 1;
    public static final int ELIXIR_RATIO_INDEX = 2;

    // Symmetry consts
    public static final int SYMMETRY_COMMS_IDX = 63;
    public static final int HORIZONTAL_SYMMETRY_MASK = 4;
    public static final int HORIZONTAL_SYMMETRY_SHIFT = 2;
    public static final int VERTICAL_SYMMETRY_MASK = 2;
    public static final int VERTICAL_SYMMETRY_SHIFT = 1;
    public static final int ROTATIONAL_SYMMETRY_MASK = 1;
    public static final int ROTATIONAL_SYMMETRY_SHIFT = 0;

    // New well comms consts
    public static final int NEW_ADAMANTIUM_WELL_COMMS_IDX = 55;
    public static final int NEW_MANA_WELL_COMMS_IDX = 56;
    public static final int NEW_ELIXIR_WELL_COMMS_IDX = 57;
    public static final int NEW_WELL_X_MASK = 4032;
    public static final int NEW_WELL_X_SHIFT = 6;
    public static final int NEW_WELL_Y_MASK = 63;
    public static final int NEW_WELL_Y_SHIFT = 0;

    // Region consts
    public static final int NUM_REGIONS_HORIZONTAL = 7;
    public static final int NUM_REGIONS_VERTICAL = 7;
    public static final int NUM_REGIONS_TOTAL = NUM_REGIONS_HORIZONTAL * NUM_REGIONS_VERTICAL;




}
