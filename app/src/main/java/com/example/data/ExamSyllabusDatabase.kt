package com.example.data

object ExamSyllabusDatabase {

    data class ChapterItem(
        val name: String,
        val classLevel: String, // "11th" or "12th"
        val pyqWeightage: String, // "High Yield (3-4 Qs)", etc.
        val yearsFeatured: String = "1988-2026",
        val branch: String = "" // "Organic", "Inorganic", "Physical", "Botany", "Zoology", "Physics"
    )

    val neetPhysicsChapters = listOf(
        ChapterItem("Units & Measurements", "11th", "Moderate (1-2 Qs)", branch = "Physics"),
        ChapterItem("Motion in a Straight Line", "11th", "Moderate (1-2 Qs)", branch = "Physics"),
        ChapterItem("Motion in a Plane (Vectors & Projectile)", "11th", "High Yield (2 Qs)", branch = "Physics"),
        ChapterItem("Laws of Motion & Friction", "11th", "High Yield (2-3 Qs)", branch = "Physics"),
        ChapterItem("Work, Energy & Power", "11th", "High Yield (2-3 Qs)", branch = "Physics"),
        ChapterItem("System of Particles & Rotational Motion", "11th", "🔥 Very High Yield (3-4 Qs)", branch = "Physics"),
        ChapterItem("Gravitation", "11th", "High Yield (2 Qs)", branch = "Physics"),
        ChapterItem("Mechanical Properties of Solids & Fluids", "11th", "High Yield (2-3 Qs)", branch = "Physics"),
        ChapterItem("Thermal Properties of Matter", "11th", "Moderate (1-2 Qs)", branch = "Physics"),
        ChapterItem("Thermodynamics & Heat Engines", "11th", "🔥 Very High Yield (3 Qs)", branch = "Physics"),
        ChapterItem("Kinetic Theory of Gases", "11th", "Moderate (1 Qs)", branch = "Physics"),
        ChapterItem("Oscillations & Waves", "11th", "High Yield (2-3 Qs)", branch = "Physics"),
        ChapterItem("Electrostatics (Charges & Potential)", "12th", "🔥 Very High Yield (3-4 Qs)", branch = "Physics"),
        ChapterItem("Current Electricity", "12th", "🔥 Very High Yield (3-4 Qs)", branch = "Physics"),
        ChapterItem("Moving Charges & Magnetism", "12th", "High Yield (2-3 Qs)", branch = "Physics"),
        ChapterItem("Magnetism & Matter", "12th", "Moderate (1 Qs)", branch = "Physics"),
        ChapterItem("Electromagnetic Induction (EMI)", "12th", "High Yield (2 Qs)", branch = "Physics"),
        ChapterItem("Alternating Current (AC)", "12th", "High Yield (2 Qs)", branch = "Physics"),
        ChapterItem("Electromagnetic Waves", "12th", "Moderate (1 Qs)", branch = "Physics"),
        ChapterItem("Ray Optics & Optical Instruments", "12th", "🔥 Very High Yield (3-4 Qs)", branch = "Physics"),
        ChapterItem("Wave Optics & Interference", "12th", "High Yield (2 Qs)", branch = "Physics"),
        ChapterItem("Dual Nature of Radiation & Matter", "12th", "High Yield (2 Qs)", branch = "Physics"),
        ChapterItem("Atoms & Nuclei", "12th", "High Yield (2-3 Qs)", branch = "Physics"),
        ChapterItem("Semiconductor Electronics", "12th", "🔥 Very High Yield (3 Qs)", branch = "Physics")
    )

    val organicChemistryChapters = listOf(
        ChapterItem("Organic Chemistry - Basic Principles & Techniques (GOC)", "11th", "🔥 Very High Yield (4 Qs)", branch = "Organic"),
        ChapterItem("Hydrocarbons (Alkanes, Alkenes, Alkynes, Aromatic)", "11th", "🔥 Very High Yield (3-4 Qs)", branch = "Organic"),
        ChapterItem("Haloalkanes and Haloarenes", "12th", "High Yield (2 Qs)", branch = "Organic"),
        ChapterItem("Alcohols, Phenols and Ethers", "12th", "High Yield (2-3 Qs)", branch = "Organic"),
        ChapterItem("Aldehydes, Ketones and Carboxylic Acids", "12th", "🔥 Very High Yield (4 Qs)", branch = "Organic"),
        ChapterItem("Amines & Diazonium Salts", "12th", "High Yield (2 Qs)", branch = "Organic"),
        ChapterItem("Biomolecules", "12th", "High Yield (2 Qs)", branch = "Organic"),
        ChapterItem("Purification & Characterization of Organic Compounds", "11th", "Moderate (1-2 Qs)", branch = "Organic")
    )

    val inorganicChemistryChapters = listOf(
        ChapterItem("Periodic Classification & Periodicity", "11th", "High Yield (2 Qs)", branch = "Inorganic"),
        ChapterItem("Chemical Bonding & Molecular Structure", "11th", "🔥 Very High Yield (4 Qs)", branch = "Inorganic"),
        ChapterItem("d and f Block Elements", "12th", "High Yield (2-3 Qs)", branch = "Inorganic"),
        ChapterItem("Coordination Compounds & Isomerism", "12th", "🔥 Very High Yield (3-4 Qs)", branch = "Inorganic"),
        ChapterItem("p-Block Elements (Group 13-18 Trends)", "12th", "High Yield (2-3 Qs)", branch = "Inorganic")
    )

    val physicalChemistryChapters = listOf(
        ChapterItem("Some Basic Concepts of Chemistry (Mole Concept)", "11th", "High Yield (2 Qs)", branch = "Physical"),
        ChapterItem("Structure of Atom", "11th", "High Yield (2 Qs)", branch = "Physical"),
        ChapterItem("Chemical & Ionic Equilibrium", "11th", "🔥 Very High Yield (3-4 Qs)", branch = "Physical"),
        ChapterItem("Thermodynamics & Thermochemistry", "11th", "High Yield (2-3 Qs)", branch = "Physical"),
        ChapterItem("Redox Reactions", "11th", "Moderate (1 Qs)", branch = "Physical"),
        ChapterItem("Solutions & Colligative Properties", "12th", "High Yield (2 Qs)", branch = "Physical"),
        ChapterItem("Electrochemistry & Nernst Equation", "12th", "🔥 Very High Yield (3 Qs)", branch = "Physical"),
        ChapterItem("Chemical Kinetics & Order of Reactions", "12th", "High Yield (2 Qs)", branch = "Physical")
    )

    val neetChemistryChapters = (organicChemistryChapters + inorganicChemistryChapters + physicalChemistryChapters)

    val neetBiologyChapters = listOf(
        ChapterItem("The Living World & Biological Classification", "11th", "High Yield (3-4 Qs)", branch = "Botany"),
        ChapterItem("Plant Kingdom (Algae to Angiosperms)", "11th", "High Yield (3 Qs)", branch = "Botany"),
        ChapterItem("Animal Kingdom & Phylum Characteristics", "11th", "🔥 Very High Yield (4-5 Qs)", branch = "Zoology"),
        ChapterItem("Morphology of Flowering Plants", "11th", "High Yield (3-4 Qs)", branch = "Botany"),
        ChapterItem("Anatomy of Flowering Plants", "11th", "High Yield (3 Qs)", branch = "Botany"),
        ChapterItem("Structural Organisation in Animals", "11th", "Moderate (2 Qs)", branch = "Zoology"),
        ChapterItem("Cell: The Unit of Life & Cell Cycle", "11th", "🔥 Very High Yield (6-7 Qs)", branch = "Botany"),
        ChapterItem("Biomolecules in Living Systems", "11th", "High Yield (3-4 Qs)", branch = "Zoology"),
        ChapterItem("Photosynthesis in Higher Plants", "11th", "High Yield (3-4 Qs)", branch = "Botany"),
        ChapterItem("Respiration in Plants & Fermentation", "11th", "Moderate (2-3 Qs)", branch = "Botany"),
        ChapterItem("Plant Growth and Development", "11th", "High Yield (3 Qs)", branch = "Botany"),
        ChapterItem("Breathing and Exchange of Gases", "11th", "High Yield (2-3 Qs)", branch = "Zoology"),
        ChapterItem("Body Fluids and Circulation", "11th", "High Yield (3 Qs)", branch = "Zoology"),
        ChapterItem("Excretory Products and Their Elimination", "11th", "High Yield (2-3 Qs)", branch = "Zoology"),
        ChapterItem("Locomotion and Movement", "11th", "High Yield (2-3 Qs)", branch = "Zoology"),
        ChapterItem("Neural Control and Chemical Coordination", "11th", "🔥 Very High Yield (4-5 Qs)", branch = "Zoology"),
        ChapterItem("Sexual Reproduction in Flowering Plants", "12th", "🔥 Very High Yield (4-5 Qs)", branch = "Botany"),
        ChapterItem("Human Reproduction & Reproductive Health", "12th", "🔥 Very High Yield (5-6 Qs)", branch = "Zoology"),
        ChapterItem("Principles of Inheritance and Variation (Genetics I)", "12th", "🔥 Super High Yield (6-8 Qs)", branch = "Botany"),
        ChapterItem("Molecular Basis of Inheritance (Genetics II)", "12th", "🔥 Super High Yield (7-9 Qs)", branch = "Botany"),
        ChapterItem("Evolution & Darwinism", "12th", "High Yield (3 Qs)", branch = "Zoology"),
        ChapterItem("Human Health and Disease", "12th", "High Yield (3-4 Qs)", branch = "Zoology"),
        ChapterItem("Microbes in Human Welfare", "12th", "Moderate (2 Qs)", branch = "Botany"),
        ChapterItem("Biotechnology: Principles and Processes", "12th", "🔥 Very High Yield (5-6 Qs)", branch = "Zoology"),
        ChapterItem("Biotechnology and its Applications", "12th", "High Yield (3-4 Qs)", branch = "Zoology"),
        ChapterItem("Organisms, Populations & Ecosystem", "12th", "🔥 Very High Yield (5-6 Qs)", branch = "Botany"),
        ChapterItem("Biodiversity and Conservation", "12th", "High Yield (3 Qs)", branch = "Botany")
    )

    fun getChaptersFor(exam: ExamCategory, subject: String): List<ChapterItem> {
        val s = subject.lowercase().trim()
        return when {
            s.contains("organic") -> organicChemistryChapters
            s.contains("inorganic") -> inorganicChemistryChapters
            s.contains("physical chem") -> physicalChemistryChapters
            s.contains("physics") -> neetPhysicsChapters
            s.contains("chemistry") -> neetChemistryChapters
            s.contains("botany") -> neetBiologyChapters.filter { it.branch == "Botany" }
            s.contains("zoology") -> neetBiologyChapters.filter { it.branch == "Zoology" }
            else -> neetBiologyChapters
        }
    }

    fun getSubjectsFor(exam: ExamCategory = ExamCategory.NEET): List<String> {
        return listOf("Physics", "Chemistry", "Biology")
    }

    fun parseExamCategory(examGoal: String): ExamCategory {
        return when (examGoal.uppercase().trim()) {
            "NEET", "NEET UG" -> ExamCategory.NEET
            "JEE", "JEE MAIN" -> ExamCategory.JEE_MAIN
            "JEE ADV", "JEE ADVANCED" -> ExamCategory.JEE_ADVANCED
            else -> ExamCategory.NEET
        }
    }

    fun getSubjectsFor(examGoalStr: String): List<String> {
        return getSubjectsFor(parseExamCategory(examGoalStr))
    }

    fun getChaptersFor(examGoalStr: String, subject: String): List<String> {
        return getChaptersFor(parseExamCategory(examGoalStr), subject).map { it.name }
    }

    fun getAllChaptersForExam(exam: ExamCategory = ExamCategory.NEET): List<ChapterItem> {
        return (neetPhysicsChapters + neetChemistryChapters + neetBiologyChapters)
    }

    val neetChapterSubtopics: Map<String, List<String>> = mapOf(
        // ==========================================
        // PHYSICS CHAPTERS (11th & 12th)
        // ==========================================
        "Units & Measurements" to listOf(
            "Physical Quantities & SI Base Units",
            "Derived Units & Systems of Units",
            "Dimensional Formulae & Dimensional Analysis",
            "Applications & Limitations of Dimensions",
            "Errors in Measurement & Propagation of Errors",
            "Significant Figures & Rounding Off Rules",
            "Measuring Instruments: Vernier Calipers & Screw Gauge",
            "Past Years PYQ Practice"
        ),
        "Motion in a Straight Line" to listOf(
            "Frame of Reference, Position & Displacement",
            "Average & Instantaneous Speed & Velocity",
            "Uniform Acceleration & Kinematic Equations",
            "Position-Time & Velocity-Time Graphs",
            "Free Fall & Vertical Motion under Gravity",
            "Relative Velocity in One Dimension",
            "Stopping Distance & Reaction Time",
            "Past Years PYQ Practice"
        ),
        "Motion in a Plane (Vectors & Projectile)" to listOf(
            "Scalars, Vectors & Vector Operations",
            "Resolution of Vectors & Unit Vectors",
            "Scalar (Dot) & Vector (Cross) Products",
            "Relative Velocity in Two Dimensions",
            "Projectile Motion: Horizontal & Angular Projection",
            "Range, Maximum Height & Time of Flight",
            "Uniform Circular Motion & Centripetal Acceleration",
            "Past Years PYQ Practice"
        ),
        "Laws of Motion & Friction" to listOf(
            "Newton's 1st Law & Inertia",
            "Newton's 2nd Law, Momentum & Impulse",
            "Newton's 3rd Law & Conservation of Momentum",
            "Equilibrium of Concurrent Forces & Free Body Diagrams",
            "Static & Kinetic Friction, Angle of Repose",
            "Dynamics of Circular Motion & Banking of Roads",
            "Pulley-Block & Constraint Systems",
            "Past Years PYQ Practice"
        ),
        "Work, Energy & Power" to listOf(
            "Work Done by Constant & Variable Force",
            "Kinetic Energy & Work-Energy Theorem",
            "Conservative vs Non-Conservative Forces",
            "Potential Energy (Gravitational & Elastic Spring)",
            "Conservation of Mechanical Energy",
            "Power (Average, Instantaneous) & Efficiency",
            "Elastic & Inelastic Collisions in 1D & 2D",
            "Past Years PYQ Practice"
        ),
        "System of Particles & Rotational Motion" to listOf(
            "Centre of Mass of Two-Particle & Continuous Systems",
            "Linear Momentum of a System & Velocity of COM",
            "Torque & Angular Momentum Conservation",
            "Moment of Inertia & Radius of Gyration",
            "Theorems of Parallel & Perpendicular Axes",
            "Rotational Kinematics & Dynamics equations",
            "Rolling Motion without Slipping on Flat & Inclined Planes",
            "Past Years PYQ Practice"
        ),
        "Gravitation" to listOf(
            "Kepler's Laws of Planetary Motion",
            "Newton's Universal Law of Gravitation & 'G'",
            "Acceleration Due to Gravity 'g' & its Variations (Altitude, Depth, Latitude)",
            "Gravitational Field & Potential Energy",
            "Escape Velocity from Planetary Surfaces",
            "Satellite Motion, Orbital Velocity & Time Period",
            "Geostationary & Polar Satellites, Weightlessness",
            "Past Years PYQ Practice"
        ),
        "Mechanical Properties of Solids & Fluids" to listOf(
            "Stress-Strain Curve & Hooke's Law",
            "Moduli of Elasticity (Young's, Bulk, Shear)",
            "Elastic Potential Energy Stored in Stretched Wire",
            "Pascal's Law & Hydraulic Machines",
            "Hydrostatic Pressure & Buoyancy (Archimedes' Principle)",
            "Streamline Flow, Equation of Continuity & Bernoulli's Principle",
            "Viscosity, Stokes' Law & Terminal Velocity",
            "Surface Tension, Surface Energy, Angle of Contact & Capillarity",
            "Past Years PYQ Practice"
        ),
        "Thermal Properties of Matter" to listOf(
            "Temperature Scales & Thermal Expansion (Alpha, Beta, Gamma)",
            "Specific Heat Capacity & Calorimetry Principle",
            "Latent Heat & Phase Changes (Melting, Vaporization)",
            "Heat Transfer: Conduction, Convection & Thermal Resistance",
            "Thermal Radiation: Blackbody Radiation & Stefan's Law",
            "Wien's Displacement Law & Spectral Distribution",
            "Newton's Law of Cooling & Applications",
            "Past Years PYQ Practice"
        ),
        "Thermodynamics & Heat Engines" to listOf(
            "Thermal Equilibrium & Zeroth Law of Thermodynamics",
            "Internal Energy & First Law of Thermodynamics",
            "Thermodynamic State Variables & Indicator Diagrams (P-V)",
            "Thermodynamic Processes: Isothermal, Adiabatic, Isobaric, Isochoric",
            "Work Done in Isothermal & Adiabatic Expansions",
            "Heat Capacities: Cp, Cv & Mayer's Relation (Cp - Cv = R)",
            "Second Law of Thermodynamics (Kelvin-Planck & Clausius)",
            "Carnot Engine, Carnot Cycle, Efficiency & Refrigerators",
            "Past Years PYQ Practice"
        ),
        "Kinetic Theory of Gases" to listOf(
            "Equation of State of a Perfect Gas & Gas Laws",
            "Postulates of Kinetic Theory & Kinetic Pressure Expression",
            "Kinetic Interpretation of Temperature & Kinetic Energy",
            "Molecular Speeds: RMS, Average & Most Probable Speeds",
            "Degrees of Freedom & Law of Equipartition of Energy",
            "Specific Heat Capacities of Monatomic, Diatomic & Polyatomic Gases",
            "Mean Free Path & Molecular Collisions",
            "Past Years PYQ Practice"
        ),
        "Oscillations & Waves" to listOf(
            "Periodic & Simple Harmonic Motion (SHM) Kinematics",
            "Differential Equation of SHM & Phase Relations",
            "Energy in SHM: Kinetic, Potential & Total Energy Conservation",
            "Simple Pendulum, Loaded Spring & Oscillating Liquid Column",
            "Damped, Forced Oscillations & Resonance",
            "Wave Characteristics, Transverse & Longitudinal Waves",
            "Speed of Sound Waves & Laplace Correction",
            "Principle of Superposition & Standing Waves in Strings/Organ Pipes",
            "Beats & Doppler Effect in Sound",
            "Past Years PYQ Practice"
        ),
        "Electrostatics (Charges & Potential)" to listOf(
            "Electric Charge, Quantization & Coulomb's Law (Vector Form)",
            "Electric Field, Field Lines & Dipole in Uniform Field",
            "Electric Flux & Gauss's Law Applications (Wire, Sheet, Sphere)",
            "Electrostatic Potential & Potential Energy of System of Charges",
            "Equipotential Surfaces & Electrostatic Shielding",
            "Capacitance, Parallel Plate Capacitor with Dielectric Slabs",
            "Combination of Capacitors (Series/Parallel) & Energy Stored",
            "Past Years PYQ Practice"
        ),
        "Current Electricity" to listOf(
            "Electric Current, Drift Velocity & Ohm's Law",
            "Resistivity, Conductivity & Temperature Dependence",
            "Series & Parallel Resistors & Color Coding",
            "EMF, Internal Resistance & Terminal Potential of Cells",
            "Grouping of Cells in Series, Parallel & Mixed",
            "Kirchhoff's Laws & Loop Rule Circuit Analysis",
            "Wheatstone Bridge & Metre Bridge Applications",
            "Potentiometer: Comparison of EMF & Internal Resistance",
            "Electrical Energy, Joule's Heating & Electric Power",
            "Past Years PYQ Practice"
        ),
        "Moving Charges & Magnetism" to listOf(
            "Magnetic Field, Lorentz Force & Motion in Magnetic Field",
            "Cyclotron Motion & Velocity Selector",
            "Biot-Savart Law: Magnetic Field of Straight Wire & Circular Loop",
            "Ampere's Circuital Law & Magnetic Field of Solenoid/Toroid",
            "Force on Current Conductor & Force between Parallel Currents",
            "Torque on Current Loop, Magnetic Dipole Moment",
            "Moving Coil Galvanometer, Sensitivity & Conversion to Ammeter/Voltmeter",
            "Past Years PYQ Practice"
        ),
        "Magnetism & Matter" to listOf(
            "Bar Magnet as Equivalent Solenoid & Magnetic Field Lines",
            "Earth's Magnetic Field & Magnetic Elements (Declination, Dip)",
            "Magnetic Properties: Diamagnetic, Paramagnetic & Ferromagnetic",
            "Curie's Law & Temperature Effect on Magnetism",
            "Hysteresis Loop, Retentivity & Coercivity",
            "Electromagnets & Permanent Magnets Characteristics",
            "Past Years PYQ Practice"
        ),
        "Electromagnetic Induction (EMI)" to listOf(
            "Magnetic Flux & Faraday's Laws of Electromagnetic Induction",
            "Lenz's Law & Conservation of Energy Principle",
            "Motional EMF & Induced Electric Field",
            "Eddy Currents, Induction Furnace & Magnetic Braking",
            "Self-Inductance, Mutual Inductance & Inductor Circuits",
            "Energy Stored in an Inductor (1/2 L I^2)",
            "AC Generator Construction & Principle",
            "Past Years PYQ Practice"
        ),
        "Alternating Current (AC)" to listOf(
            "AC Voltage & Current: Peak, RMS & Average Values",
            "AC Circuit with Pure Resistor, Inductor & Capacitor (Phasors)",
            "Series LCR Circuit, Impedance & Phase Angle",
            "Resonance in LCR Circuit, Resonant Frequency & Bandwidth",
            "Quality Factor (Q-factor) & Sharpness of Resonance",
            "Power in AC Circuit, Power Factor & Wattless Current",
            "LC Oscillations & Energy Exchange",
            "Transformers: Principle, Step-up/down, Efficiency & Energy Losses",
            "Past Years PYQ Practice"
        ),
        "Electromagnetic Waves" to listOf(
            "Displacement Current & Inconsistency of Ampere's Law",
            "Maxwell's Equations Overview & EM Wave Generation",
            "Characteristics & Transverse Nature of Electromagnetic Waves",
            "Relation between Electric & Magnetic Fields (E0/B0 = c)",
            "Electromagnetic Spectrum (Radio, Micro, IR, Visible, UV, X-rays, Gamma)",
            "Wavelength Ranges, Production & Uses of EM Radiations",
            "Past Years PYQ Practice"
        ),
        "Ray Optics & Optical Instruments" to listOf(
            "Reflection of Light, Spherical Mirrors & Mirror Formula",
            "Refraction of Light, Snell's Law, Real & Apparent Depth",
            "Total Internal Reflection (TIR), Critical Angle & Optical Fibres",
            "Refraction at Spherical Surfaces & Lens Maker's Formula",
            "Thin Lenses in Contact & Lens-Mirror Combinations",
            "Refraction & Dispersion through Triangular Prism",
            "Optical Instruments: Compound Microscope & Magnifying Power",
            "Optical Instruments: Astronomical & Terrestrial Telescopes",
            "Past Years PYQ Practice"
        ),
        "Wave Optics & Interference" to listOf(
            "Huygens' Principle, Wavefronts & Reflection/Refraction Proofs",
            "Coherent Sources & Principle of Superposition",
            "Young's Double Slit Experiment (YDSE) Setup & Fringe Width",
            "Constructive & Destructive Interference Conditions",
            "Diffraction at a Single Slit & Central Maxima Width",
            "Polarization of Light, Brewster's Law & Malus's Law",
            "Resolving Power of Microscope & Telescope",
            "Past Years PYQ Practice"
        ),
        "Dual Nature of Radiation & Matter" to listOf(
            "Photoelectric Effect Experiments (Hertz, Lenard, Hallwachs)",
            "Characteristics: Threshold Frequency & Stopping Potential",
            "Einstein's Photoelectric Equation & Energy Conservation",
            "Photon Picture of Electromagnetic Radiation",
            "De Broglie Hypothesis & Matter Wave Wavelength Formula",
            "Davisson and Germer Experiment & Electron Diffraction",
            "Past Years PYQ Practice"
        ),
        "Atoms & Nuclei" to listOf(
            "Alpha Particle Scattering Experiment & Rutherford's Atomic Model",
            "Bohr's Model of Hydrogen Atom: Radii, Velocity & Energy Levels",
            "Hydrogen Spectral Lines (Lyman, Balmer, Paschen, Brackett, Pfund)",
            "Composition of Nucleus, Nuclear Radius & Nuclear Density",
            "Mass Defect, Binding Energy & Binding Energy per Nucleon Curve",
            "Nuclear Forces Characteristics (Strong, Short-Range, Saturated)",
            "Radioactivity: Alpha, Beta, Gamma Decay Laws & Half-Life Calculations",
            "Nuclear Fission, Chain Reaction & Nuclear Fusion in Stars",
            "Past Years PYQ Practice"
        ),
        "Semiconductor Electronics" to listOf(
            "Energy Bands in Solids: Valence Band, Conduction Band & Band Gap",
            "Intrinsic & Extrinsic Semiconductors (n-type, p-type Doping)",
            "p-n Junction Diode Formation, Depletion Layer & Barrier Potential",
            "Diode V-I Characteristics in Forward & Reverse Bias",
            "Semiconductor Diode as Half-Wave & Full-Wave Rectifiers (Filters)",
            "Optoelectronic Devices: Photodiode, LED & Solar Cell",
            "Zener Diode as a Voltage Regulator",
            "Logic Gates (AND, OR, NOT, NAND, NOR) & Truth Tables",
            "Past Years PYQ Practice"
        ),

        // ==========================================
        // CHEMISTRY CHAPTERS (Organic, Inorganic, Physical)
        // ==========================================
        "Organic Chemistry - Basic Principles & Techniques (GOC)" to listOf(
            "IUPAC Nomenclature of Branched & Polyfunctional Compounds",
            "Inductive Effect & Electromeric Effect",
            "Resonance, Mesomeric Effect & Hyperconjugation",
            "Carbocations, Carbanions & Carbon Free Radicals Stability",
            "Structural Isomerism (Chain, Position, Functional, Metamerism, Tautomerism)",
            "Geometrical Isomerism (cis/trans, E/Z System)",
            "Optical Isomerism (Chirality, Enantiomers, Diastereomers, Racemic)",
            "Acidic & Basic Strength Trends of Organic Compounds",
            "Past Years PYQ Practice"
        ),
        "Hydrocarbons (Alkanes, Alkenes, Alkynes, Aromatic)" to listOf(
            "Alkanes: Preparation (Wurtz, Kolbe), Conformations of Ethane",
            "Alkanes: Free Radical Halogenation Mechanism & Combustion",
            "Alkenes: Preparation (Dehydrohalogenation, Saytzeff Rule)",
            "Alkenes: Electrophilic Addition (Markovnikov vs Kharasch/Peroxide)",
            "Alkenes: Ozonolysis, Oxidation (Baeyer's Reagent) & Polymerization",
            "Alkynes: Acidity of Terminal Alkynes, Preparation & Additions",
            "Aromatic Hydrocarbons: Huckel's Rule (4n+2 pi electrons) & Aromaticity",
            "Electrophilic Aromatic Substitution: Nitration, Sulphonation, Friedel-Crafts",
            "Directing Influence of Functional Groups in Monosubstituted Benzene",
            "Past Years PYQ Practice"
        ),
        "Haloalkanes and Haloarenes" to listOf(
            "Nomenclature & Classification of Haloalkanes/Haloarenes",
            "Methods of Preparation from Alcohols, Hydrocarbons & Sandmeyer",
            "Nucleophilic Substitution: SN1 vs SN2 Mechanisms & Stereochemistry",
            "Elimination Reactions: E1 vs E2 & Zaitsev's Rule",
            "Reactions with Metals (Grignard Reagents, Wurtz-Fittig Reaction)",
            "Haloarenes: Low Reactivity towards Nucleophilic Substitution",
            "Electrophilic Substitution Reactions of Haloarenes",
            "Polyhalogen Compounds (Chloroform, Iodoform, Freons, DDT)",
            "Past Years PYQ Practice"
        ),
        "Alcohols, Phenols and Ethers" to listOf(
            "Classification & IUPAC Nomenclature of Alcohols/Phenols/Ethers",
            "Preparation of Alcohols: Hydration, Hydroboration-Oxidation, Carbonyl Reduction",
            "Properties & Reactions of Alcohols: Lucas Test, Dehydration to Alkenes",
            "Preparation of Phenols: From Cumene, Chlorobenzene & Diazonium Salts",
            "Acidic Nature of Phenols & Comparison with Alcohols",
            "Electrophilic Substitution of Phenols: Kolbe's & Reimer-Tiemann Reactions",
            "Preparation of Ethers: Williamson Synthesis & Acidic Dehydration",
            "Reactions of Ethers: Cleavage by Hydrogen Halides (HI)",
            "Past Years PYQ Practice"
        ),
        "Aldehydes, Ketones and Carboxylic Acids" to listOf(
            "Nomenclature & Structure of Carbonyl Group",
            "Preparation of Aldehydes: Rosenmund, Stephen's & Etard Reactions",
            "Preparation of Ketones: From Nitriles, Grignard & Friedel-Crafts Acylation",
            "Nucleophilic Addition Reactions (HCN, NaHSO3, Grignard, Alcohols, Amines)",
            "Reactions at Alpha-Carbon: Aldol & Cross-Aldol Condensations",
            "Cannizzaro Reaction & Haloform/Iodoform Reaction Test",
            "Distinction Tests: Tollens' Test, Fehling's Test & Brady's Reagent",
            "Carboxylic Acids: Preparation, Acidic Strength & Substituent Effects",
            "Reactions of Carboxylic Acids: HVZ Reaction & Decarboxylation",
            "Past Years PYQ Practice"
        ),
        "Amines & Diazonium Salts" to listOf(
            "Classification & Structure of Amines",
            "Preparation: Gabriel Phthalimide, Hoffmann Bromamide Degradation",
            "Preparation: Reduction of Nitro Compounds, Nitriles & Amides",
            "Basic Character of Amines: Gas Phase vs Aqueous Phase Basicity Trends",
            "Chemical Reactions: Carbylamine Reaction & Hinsberg's Test for 1°, 2°, 3° Amines",
            "Reaction with Nitrous Acid & Diazotization",
            "Benzenediazonium Salts: Preparation, Sandmeyer, Gatterman & Coupling Reactions",
            "Past Years PYQ Practice"
        ),
        "Biomolecules" to listOf(
            "Carbohydrates: Classification, Monosaccharides (Glucose & Fructose)",
            "Open vs Cyclic Haworth Structures of Glucose & Fructose",
            "Disaccharides (Sucrose, Maltose, Lactose & Glycosidic Linkage)",
            "Polysaccharides (Starch, Cellulose, Glycogen & Biological Role)",
            "Proteins: Amino Acids (Essential/Non-Essential, Zwitterion)",
            "Peptide Bonds & Protein Structures (Primary, Secondary, Tertiary, Quaternary)",
            "Denaturation of Proteins & Enzyme Action Mechanism",
            "Nucleic Acids: DNA, RNA Chemical Composition, Double Helix & Genetic Code",
            "Vitamins: Classification (Fat-Soluble vs Water-Soluble) & Deficiencies",
            "Past Years PYQ Practice"
        ),
        "Purification & Characterization of Organic Compounds" to listOf(
            "Purification Methods: Crystallization, Sublimation, Fractional & Steam Distillation",
            "Chromatography Techniques (Paper, Column, Thin Layer TLC)",
            "Qualitative Analysis: Lassaigne's Test for N, S, Halogens",
            "Quantitative Analysis: Carbon & Hydrogen Estimation (Liebig's Method)",
            "Estimation of Nitrogen (Dumas Method & Kjeldahl's Method)",
            "Estimation of Halogens & Sulphur (Carius Method)",
            "Estimation of Phosphorus & Calculations of Empirical/Molecular Formulae",
            "Past Years PYQ Practice"
        ),
        "Periodic Classification & Periodicity" to listOf(
            "Modern Periodic Law & Present Form of Periodic Table",
            "Electronic Configuration in Periods & Groups (s, p, d, f blocks)",
            "Periodic Trends in Atomic Radii & Ionic Radii (Isoelectronic Species)",
            "Ionization Enthalpy: First, Second IE & Successive Factors",
            "Electron Gain Enthalpy Trends & Anomalies (O vs S, F vs Cl)",
            "Electronegativity Scales (Pauling) & Periodic Variations",
            "Valence States, Diagonal Relationships & Anomalous First Element Behavior",
            "Past Years PYQ Practice"
        ),
        "Chemical Bonding & Molecular Structure" to listOf(
            "Kossel-Lewis Approach, Octet Rule & Formal Charge Calculations",
            "Ionic Bonding, Lattice Enthalpy & Born-Haber Cycle",
            "Covalent Bond, Coordinate Bond & Bond Parameters (Length, Angle, Enthalpy)",
            "VSEPR Theory: Geometry & Shapes of Molecules with Lone Pairs",
            "Valence Bond Theory: Orbital Overlap Concept, Sigma & Pi Bonds",
            "Hybridization (sp, sp2, sp3, sp3d, sp3d2) & Geometries of Molecules",
            "Molecular Orbital Theory (MOT): LCAO Concept & Energy Level Diagrams",
            "Bond Order & Magnetic Properties of Homonuclear Diatomic Molecules (O2, N2, etc.)",
            "Hydrogen Bonding: Intermolecular vs Intramolecular H-bonding & Dipole Moment",
            "Past Years PYQ Practice"
        ),
        "d and f Block Elements" to listOf(
            "Electronic Configurations of 3d, 4d, 5d Series Transition Elements",
            "General Trends: Atomic/Ionic Sizes, Ionization Enthalpy, Density",
            "Variable Oxidation States & Standard Electrode Potentials (E°)",
            "Magnetic Properties: Spin-Only Magnetic Moment Formula (sqrt[n(n+2)])",
            "Formation of Colored Ions, Complex Formation & Catalytic Properties",
            "Interstitial Compounds & Alloy Formation",
            "Preparation, Properties & Oxidizing Action of K2Cr2O7 and KMnO4",
            "Lanthanoids: Electronic Configurations, Oxidation States & Lanthanoid Contraction",
            "Actinoids: General Characteristics & Comparison with Lanthanoids",
            "Past Years PYQ Practice"
        ),
        "Coordination Compounds & Isomerism" to listOf(
            "Werner's Coordination Theory (Primary & Secondary Valencies)",
            "Ligands (Mono, Bi, Polydentate, Ambidentate, Chelating Ligands)",
            "Coordination Number, Oxidation State & IUPAC Nomenclature",
            "Structural Isomerism: Ionization, Solvate/Hydrate, Linkage, Coordination",
            "Stereoisomerism: Geometrical (cis/trans, fac/mer) & Optical Isomerism",
            "Valence Bond Theory (VBT): Inner & Outer Orbital Complexes, Hybridization",
            "Crystal Field Theory (CFT): Crystal Field Splitting in Octahedral & Tetrahedral Complexes",
            "Spectrochemical Series, CFSE & High Spin vs Low Spin Complexes",
            "Stability of Coordination Complexes & Applications in Analytical Chemistry",
            "Past Years PYQ Practice"
        ),
        "p-Block Elements (Group 13-18 Trends)" to listOf(
            "Group 13 (Boron Family): Electronic Configuration, Inert Pair Effect, Diborane & Boric Acid",
            "Group 14 (Carbon Family): Allotropes of Carbon, Catenation, Carbides, Silicates & Silicones",
            "Group 15 (Nitrogen Family): Anomalous N, Ammonia Haber Process, Nitric Acid Ostwald Process & Oxides of N",
            "Group 16 (Oxygen Family): Allotropes of Sulphur, Sulphuric Acid Contact Process & Oxoacids of Sulphur",
            "Group 17 (Halogen Family): Anomalous Fluorine, Interhalogen Compounds & Oxoacids of Halogens",
            "Group 18 (Noble Gases): Electronic Configurations, Xenon Fluorides/Oxides Structures (XeF2, XeF4, XeF6, XeO3)",
            "Past Years PYQ Practice"
        ),
        "Some Basic Concepts of Chemistry (Mole Concept)" to listOf(
            "Nature of Matter, Laws of Chemical Combinations",
            "Dalton's Atomic Theory & Modern Atomic Concept",
            "Atomic Mass, Molecular Mass, Formula Mass & Average Atomic Mass",
            "Mole Concept, Molar Mass, Avogadro Constant & Volume of Gas at STP",
            "Percentage Composition, Empirical Formula & Molecular Formula",
            "Stoichiometric Calculations & Limiting Reagent Concept",
            "Concentration Terms: Molarity, Molality, Normality, Mole Fraction, ppm",
            "Past Years PYQ Practice"
        ),
        "Structure of Atom" to listOf(
            "Subatomic Particles: Discovery of Electron, Proton, Neutron",
            "Thomson & Rutherford Atomic Models and their Limitations",
            "Dual Character of Electromagnetic Radiation: Planck's Quantum Theory & Photoelectric Effect",
            "Bohr's Model of Hydrogen Atom: Postulates, Radii & Energy Expressions",
            "Line Spectrum of Hydrogen Atom (Rydberg Formula)",
            "Dual Nature of Matter: De Broglie Relation & Heisenberg's Uncertainty Principle",
            "Quantum Mechanical Model: Wave Functions, Orbitals & Quantum Numbers (n, l, m, s)",
            "Shapes of s, p, d, f Orbitals & Nodal Surfaces",
            "Rules for Electronic Configuration: Aufbau Principle, Pauli Exclusion & Hund's Rule",
            "Past Years PYQ Practice"
        ),
        "Chemical & Ionic Equilibrium" to listOf(
            "Equilibrium in Physical & Chemical Processes, Dynamic Nature",
            "Law of Mass Action, Equilibrium Constants (Kc, Kp) & Reaction Quotient (Qc)",
            "Factors Affecting Equilibrium: Le Chatelier's Principle (Conc., Temp., Press., Catalyst)",
            "Theories of Acids and Bases: Arrhenius, Bronsted-Lowry & Lewis Concepts",
            "Ionization of Water, Ionic Product (Kw) & pH Scale Calculations",
            "Ionization Constants of Weak Acids (Ka) & Weak Bases (Kb)",
            "Common Ion Effect & Buffer Solutions (Henderson-Hasselbalch Equation)",
            "Hydrolysis of Salts (Acidic, Basic, Neutral Salts & pH Expressions)",
            "Solubility Product (Ksp) & Applications in Selective Precipitation",
            "Past Years PYQ Practice"
        ),
        "Thermodynamics & Thermochemistry" to listOf(
            "System, Surroundings, Types of Systems & State Functions",
            "First Law of Thermodynamics: delta U = q + w & Work Done (Reversible/Irreversible)",
            "Enthalpy (H), Relation between delta H and delta U (delta H = delta U + delta ng RT)",
            "Heat Capacity, Specific Heat & Relation between Cp and Cv",
            "Thermochemistry: Enthalpy of Reaction, Formation, Combustion, Neutralization, Bond Energy",
            "Hess's Law of Constant Heat Summation & Thermochemical Cycles",
            "Spontaneity & Second Law of Thermodynamics: Entropy (delta S)",
            "Gibbs Free Energy (delta G), Spontaneity Criterion (delta G = delta H - T delta S)",
            "Gibbs Energy Change & Equilibrium Constant (delta G° = -RT ln K)",
            "Third Law of Thermodynamics & Residual Entropy",
            "Past Years PYQ Practice"
        ),
        "Redox Reactions" to listOf(
            "Classical & Electronic Concepts of Oxidation and Reduction",
            "Oxidation Number Rules & Determination of Oxidation States",
            "Types of Redox Reactions: Combination, Decomposition, Displacement, Disproportionation",
            "Balancing Redox Reactions: Oxidation Number Method",
            "Balancing Redox Reactions: Ion-Electron / Half-Reaction Method in Acidic & Basic Medium",
            "Redox Titrations: Standard KMnO4 and K2Cr2O7 Titrations",
            "Electrode Processes & Electrochemical Concept of Redox Systems",
            "Past Years PYQ Practice"
        ),
        "Solutions & Colligative Properties" to listOf(
            "Types of Solutions & Methods of Expressing Concentrations",
            "Solubility of Gases in Liquids: Henry's Law & Applications",
            "Vapour Pressure of Liquid Solutions & Raoult's Law (Ideal Solutions)",
            "Non-Ideal Solutions: Positive & Negative Deviations from Raoult's Law & Azeotropes",
            "Colligative Property 1: Relative Lowering of Vapour Pressure",
            "Colligative Property 2: Elevation of Boiling Point (Molal Elevation Constant Kb)",
            "Colligative Property 3: Depression of Freezing Point (Molal Cryoscopic Constant Kf)",
            "Colligative Property 4: Osmotic Pressure, Van't Hoff Law & Reverse Osmosis",
            "Abnormal Molar Mass & Van't Hoff Factor 'i' for Dissociation and Association",
            "Past Years PYQ Practice"
        ),
        "Electrochemistry & Nernst Equation" to listOf(
            "Electrochemical Cells (Galvanic / Voltaic Cells: Daniell Cell Setup)",
            "Electrode Potentials, Standard Hydrogen Electrode (SHE) & Electrochemical Series",
            "Nernst Equation for Single Electrode & Complete Cell EMF",
            "Relationship between Cell Potential, Gibbs Free Energy (delta G = -nFE) & Equilibrium Constant",
            "Electrolytic Conductance: Resistance, Resistivity, Specific & Molar Conductance",
            "Variation of Conductivity & Molar Conductivity with Concentration",
            "Kohlrausch's Law of Independent Migration of Ions & Applications",
            "Electrolysis: Faraday's 1st & 2nd Laws of Electrolysis",
            "Batteries: Primary Cells (Dry Cell, Mercury) & Secondary Cells (Lead Storage, Ni-Cd)",
            "Fuel Cells (H2-O2 Cell) & Mechanism of Corrosion and Prevention",
            "Past Years PYQ Practice"
        ),
        "Chemical Kinetics & Order of Reactions" to listOf(
            "Rate of a Chemical Reaction: Average, Instantaneous & Initial Rates",
            "Factors Influencing Reaction Rates: Concentration, Temperature, Catalyst",
            "Rate Law, Specific Rate Constant (k) & Rate Equation",
            "Order of a Reaction vs Molecularity of an Elementary Reaction",
            "Integrated Rate Equation & Half-Life for Zero Order Reactions",
            "Integrated Rate Equation & Half-Life for First Order Reactions",
            "Pseudo First Order Reactions & Radioactive Decay as 1st Order",
            "Temperature Dependence of Rate Constant: Arrhenius Equation & Activation Energy (Ea)",
            "Collision Theory of Chemical Reactions & Transition State Concept",
            "Past Years PYQ Practice"
        ),

        // ==========================================
        // BIOLOGY CHAPTERS (Botany & Zoology)
        // ==========================================
        "The Living World & Biological Classification" to listOf(
            "What is Living? Characteristics: Growth, Reproduction, Metabolism, Consciousness",
            "Biodiversity, Need for Classification & Binomial Nomenclature Rules",
            "Taxonomic Hierarchy: Species, Genus, Family, Order, Class, Phylum, Kingdom",
            "Taxonomical Aids: Herbarium, Botanical Gardens, Museums, Zoos & Keys",
            "Whittaker's Five Kingdom System Criteria",
            "Kingdom Monera: Archaebacteria, Eubacteria, Cyanobacteria & Mycoplasma",
            "Kingdom Protista: Chrysophytes, Dinoflagellates, Euglenoids, Slime Moulds, Protozoans",
            "Kingdom Fungi: Phycomycetes, Ascomycetes, Basidiomycetes & Deuteromycetes",
            "Viruses, Viroids, Prions & Lichens Structure and Economic Role",
            "Past Years PYQ Practice"
        ),
        "Plant Kingdom (Algae to Angiosperms)" to listOf(
            "Systems of Classification: Artificial, Natural & Phylogenetic",
            "Algae: Chlorophyceae (Green), Phaeophyceae (Brown) & Rhodophyceae (Red)",
            "Algae: Pigments, Stored Food, Cell Wall & Reproduction Patterns",
            "Bryophytes: Liverworts (Marchantia) & Mosses (Funaria, Sphagnum) - Life Cycles",
            "Pteridophytes: Homosporous vs Heterosporous (Selaginella, Salvinia) & Seed Habit Origin",
            "Gymnosperms: Cycas, Pinus, Morphology, Anatomy, Mycorrhizae & Ovule Structure",
            "Angiosperms Overview: Monocots vs Dicots, Double Fertilization",
            "Plant Life Cycles & Alternation of Generations (Haplontic, Diplontic, Haplodiplontic)",
            "Past Years PYQ Practice"
        ),
        "Animal Kingdom & Phylum Characteristics" to listOf(
            "Basis of Animal Classification: Symmetry, Coelom, Germ Layers, Notochord",
            "Phylum Porifera (Sponges): Cellular Level, Choanocytes, Canal System, Spicules",
            "Phylum Cnidaria / Coelenterata: Cnidoblasts, Polyp vs Medusa, Metagenesis (Obelia)",
            "Phylum Ctenophora: Comb Plates, Bioluminescence & Phylum Platyhelminthes (Flame Cells)",
            "Phylum Aschelminthes: Pseudocoelomates (Ascaris, Wuchereria) & Phylum Annelida (Metamerism)",
            "Phylum Arthropoda: Jointed Appendages, Chitinous Exoskeleton, Malpighian Tubules",
            "Phylum Mollusca (Radula, Mantle) & Phylum Echinodermata (Water Vascular System)",
            "Phylum Hemichordata & Chordata Characteristics",
            "Subphyla: Urochordata, Cephalochordata & Vertebrata",
            "Classes of Gnathostomata: Chondrichthyes vs Osteichthyes, Amphibia, Reptilia, Aves, Mammalia",
            "Past Years PYQ Practice"
        ),
        "Morphology of Flowering Plants" to listOf(
            "Root System: Tap Root, Fibrous Root, Adventitious Roots & Modifications",
            "Stem: Characteristics & Modifications (Tendrils, Thorns, Rhizome, Tuber, Offset)",
            "Leaf: Structure, Venation (Reticulate, Parallel), Types & Phyllotaxy",
            "Inflorescence: Racemose vs Cymose Inflorescences",
            "Flower: Parts, Symmetry (Actinomorphic, Zygomorphic), Epigynous/Hypogynous/Perigynous",
            "Calyx, Corolla & Aestivation (Valvate, Twisted, Imbricate, Vexillary)",
            "Androecium: Cohesion, Adhesion & Gynoecium: Types of Placentation",
            "Fruit: True, False, Parthenocarpic, Drupe Structure & Seed Structure (Dicot/Monocot)",
            "Semi-Technical Description & Diagnostic Families (Solanaceae, Fabaceae, Liliaceae, Malvaceae, Poaceae)",
            "Past Years PYQ Practice"
        ),
        "Anatomy of Flowering Plants" to listOf(
            "Meristematic Tissues: Apical, Intercalary, Lateral (Primary vs Secondary)",
            "Simple Permanent Tissues: Parenchyma, Collenchyma, Sclerenchyma",
            "Complex Permanent Tissues: Xylem (Tracheids, Vessels, Fibres, Parenchyma)",
            "Complex Permanent Tissues: Phloem (Sieve Tubes, Companion Cells, Phloem Parenchyma)",
            "Tissue Systems: Epidermal (Stomata, Trichomes), Ground & Vascular Systems",
            "Anatomy of Dicotyledonous & Monocotyledonous Roots",
            "Anatomy of Dicotyledonous & Monocotyledonous Stems",
            "Anatomy of Dorsiventral (Dicot) & Isobilateral (Monocot) Leaves",
            "Secondary Growth in Dicot Stems: Vascular Cambium, Cork Cambium, Heartwood vs Sapwood",
            "Past Years PYQ Practice"
        ),
        "Structural Organisation in Animals" to listOf(
            "Epithelial Tissue: Simple (Squamous, Cuboidal, Columnar, Ciliated) & Compound",
            "Glandular Epithelium (Exocrine, Endocrine) & Cell Junctions (Tight, Adhering, Gap)",
            "Connective Tissue: Loose (Areolar, Adipose) & Dense (Tendons, Ligaments)",
            "Specialized Connective Tissue: Cartilage, Bone & Haversian Canal System, Blood",
            "Muscle Tissue: Skeletal (Striated), Smooth (Visceral) & Cardiac Muscles",
            "Neural Tissue: Neurons, Neuroglia & Nerve Impulse Reception",
            "Morphology & Complete Anatomy of Frog: Digestive, Respiratory, Circulatory, Excretory & Nervous Systems",
            "Past Years PYQ Practice"
        ),
        "Cell: The Unit of Life & Cell Cycle" to listOf(
            "Cell Theory & Overview of Prokaryotic vs Eukaryotic Cells",
            "Prokaryotic Cell Structure: Cell Envelope, Mesosomes, Plasmids, Ribosomes, Inclusion Bodies",
            "Plasma Membrane: Fluid Mosaic Model & Transport Mechanisms",
            "Cell Wall, Middle Lamella, Plasmodesmata & Endomembrane System (ER, Golgi, Lysosomes, Vacuoles)",
            "Mitochondria (Cristae, Matrix) & Chloroplasts (Thylakoids, Grana, Stroma)",
            "Ribosomes (70S, 80S), Cytoskeleton, Cilia, Flagella, Centrosome & Centrioles",
            "Nucleus: Nuclear Membrane, Nucleolus, Chromatin & Chromosome Types (Metacentric, Submetacentric, etc.)",
            "Cell Cycle Phases: Interphase (G1, S, G2) & M-Phase",
            "Mitosis Stages: Prophase, Metaphase, Anaphase, Telophase & Cytokinesis",
            "Meiosis I (Detailed Prophase I: Leptotene, Zygotene, Pachytene, Diplotene, Diakinesis)",
            "Meiosis II & Significance of Meiosis in Genetic Variation",
            "Past Years PYQ Practice"
        ),
        "Biomolecules in Living Systems" to listOf(
            "Analysis of Chemical Constituents: Acid-Soluble vs Acid-Insoluble Fractions",
            "Amino Acids: Structure, Zwitterion Form & Classification",
            "Lipids: Fatty Acids (Saturated/Unsaturated), Glycerides, Phospholipids (Lecithin)",
            "Carbohydrates & Nucleic Acids Building Blocks",
            "Primary vs Secondary Metabolites (Alkaloids, Terpenoids, Toxins, Drugs, Lectins)",
            "Nature of Bond Linking Monomers in Polymers",
            "Proteins Structure (1°, 2°, 3°, 4°) & Dynamic State of Body Constituents",
            "Enzymes: Chemical Reactions, Activation Energy & Lock-and-Key / Induced-Fit Models",
            "Factors Affecting Enzyme Activity: Temperature, pH, Substrate Conc. (Km value)",
            "Enzyme Inhibition: Competitive (Malonate) vs Non-Competitive Inhibition & Co-factors",
            "Past Years PYQ Practice"
        ),
        "Photosynthesis in Higher Plants" to listOf(
            "Historical Experiments: Priestley, Ingenhousz, Senebier, Engelmann, Van Niel",
            "Photosynthetic Pigments: Chlorophyll a, b, Carotenoids, Absorption vs Action Spectra",
            "Light Reaction: Light Harvesting Complex (LHC), PS I, PS II",
            "Electron Transport: Z-scheme, Cyclic & Non-Cyclic Photophosphorylation",
            "Chemiosmotic Hypothesis of ATP Synthesis (Proton Gradient & ATP Synthase)",
            "Dark Reaction (Calvin C3 Cycle): Carboxylation, Reduction, Regeneration of RuBP",
            "C4 Pathway (Hatch-Slack): Kranz Anatomy, PEPcase & Bundle Sheath Cells",
            "Photorespiration (C2 Cycle) & Differences between C3 and C4 Plants",
            "Factors Affecting Photosynthesis: Blackman's Law of Limiting Factors",
            "Past Years PYQ Practice"
        ),
        "Respiration in Plants & Fermentation" to listOf(
            "Cellular Respiration Concept & Respiratory Substrates",
            "Glycolysis (EMP Pathway): 10 Enzymatic Steps, Energy Investment & Generation",
            "Fermentation: Anaerobic Alcoholic & Lactic Acid Pathways",
            "Aerobic Respiration: Link Reaction / Oxidative Decarboxylation of Pyruvate",
            "TCA Cycle / Krebs Cycle: Cyclic Steps, Energy Yield & Key Dehydrogenases",
            "Electron Transport System (ETS Complexes I-V) & Oxidative Phosphorylation",
            "Respiratory Balance Sheet: Net ATP Production from Glucose Breakdown",
            "Amphibolic Pathway Concept & Respiratory Quotient (RQ Values for Carbohydrates, Fats, Proteins)",
            "Past Years PYQ Practice"
        ),
        "Plant Growth and Development" to listOf(
            "Growth Characteristics, Phases of Growth (Meristematic, Elongation, Maturation)",
            "Growth Rates: Arithmetic vs Geometric Growth, Sigmoid Growth Curve",
            "Differentiation, Dedifferentiation, Redifferentiation & Plasticity (Heterophylly)",
            "Plant Growth Regulators (PGRs): Auxins (Apical Dominance, Uses)",
            "PGRs: Gibberellins (Bolting, Stem Elongation), Cytokinins (Cell Division, Delay of Senescence)",
            "PGRs: Ethylene (Fruit Ripening, Triple Response) & Abscisic Acid (ABA - Stress Hormone)",
            "Photoperiodism: Short-Day, Long-Day & Day-Neutral Plants, Florigen",
            "Vernalization & Seed Dormancy Mechanisms",
            "Past Years PYQ Practice"
        ),
        "Breathing and Exchange of Gases" to listOf(
            "Human Respiratory System Anatomy: Nostrils, Pharynx, Larynx, Trachea, Bronchi, Alveoli",
            "Mechanism of Breathing: Inspiration, Expiration, Diaphragm & Intercostal Muscles",
            "Respiratory Volumes: Tidal Volume (TV), IRV, ERV, Residual Volume (RV)",
            "Respiratory Capacities: IC, EC, FRC, Vital Capacity (VC), Total Lung Capacity (TLC)",
            "Exchange of Gases: Partial Pressures (pO2, pCO2) Across Alveolar-Capillary Membrane",
            "Transport of Oxygen: Oxyhemoglobin & Oxygen-Hemoglobin Dissociation Curve (Bohr Effect)",
            "Transport of Carbon Dioxide: Bicarbonate Ions, Carbaminohemoglobin, Chloride Shift",
            "Regulation of Respiration: Respiratory Rhythm Centre, Pneumotaxic Centre & Chemosensitive Area",
            "Respiratory Disorders: Asthma, Emphysema (Alpha-1 Antitrypsin), Occupational Disorders",
            "Past Years PYQ Practice"
        ),
        "Body Fluids and Circulation" to listOf(
            "Blood Composition: Plasma (Fibrinogen, Globulins, Albumin) & Formed Elements (RBC, WBC, Platelets)",
            "ABO Blood Grouping, Rh Factor & Erythroblastosis Foetalis",
            "Coagulation of Blood: Clotting Cascade, Thrombokinase, Prothrombin to Thrombin",
            "Lymph (Tissue Fluid): Formation, Composition & Immune Functions",
            "Human Circulatory System: Internal Anatomy of Heart, Heart Valves & Chambers",
            "Nodal Tissues: Sinoatrial Node (SA Node - Pacemaker), AV Node, Purkinje Fibres",
            "Cardiac Cycle: Atrial/Ventricular Systole & Joint Diastole, Stroke Volume, Cardiac Output",
            "Heart Sounds: Lubb & Dupp Origin",
            "Electrocardiogram (ECG): P Wave, QRS Complex, T Wave Analysis",
            "Double Circulation: Systemic & Pulmonary Loops, Hepatic Portal System",
            "Regulation of Heart: Neural & Hormonal Controls & Circulatory Disorders (Hypertension, CAD, Angina, Heart Failure)",
            "Past Years PYQ Practice"
        ),
        "Excretory Products and Their Elimination" to listOf(
            "Modes of Excretion: Ammonotelism, Ureotelism, Uricotelism",
            "Human Excretory System: Gross Kidney Anatomy, Cortex, Medulla, Calyces, Pelvis, Ureter",
            "Structure of Nephron: Glomerulus, Bowman's Capsule, PCT, Loop of Henle, DCT, Collecting Duct",
            "Types of Nephrons: Cortical vs Juxtamedullary Nephrons",
            "Mechanism of Urine Formation 1: Glomerular Ultrafiltration (GFR & Net Filtration Pressure)",
            "Mechanism of Urine Formation 2: Selective Tubular Reabsorption (Active vs Passive)",
            "Mechanism of Urine Formation 3: Tubular Secretion & Ionic Balance",
            "Mechanism of Concentration: Counter-Current Multiplier in Henle's Loop & Vasa Recta",
            "Regulation of Kidney Function: Hypothalamus & ADH/Vasopressin",
            "Regulation of Kidney Function: Renin-Angiotensin-Aldosterone System (RAAS) & ANF",
            "Micturition Reflex & Role of Lungs, Liver, Skin in Excretion",
            "Excretory Disorders: Uremia, Renal Calculi, Glomerulonephritis & Hemodialysis / Kidney Transplant",
            "Past Years PYQ Practice"
        ),
        "Locomotion and Movement" to listOf(
            "Types of Movements in Humans: Amoeboid, Ciliary, Flagellar, Muscular",
            "Structure of Skeletal Muscle: Muscle Bundles, Fascicles, Muscle Fibres, Sarcolemma",
            "Ultrastructure of Myofilaments: Actin (F-actin, Tropomyosin, Troponin) & Myosin (Meromyosin)",
            "Sarcomere Anatomy: Z-Line, I-Band, A-Band, H-Zone, M-Line",
            "Mechanism of Muscle Contraction: Sliding Filament Theory & Cross-Bridge Cycle",
            "Role of Calcium Ions, Troponin Binding, ATP Hydrolysis & Relaxation",
            "Red Muscle Fibres (Myoglobin Rich) vs White Muscle Fibres",
            "Human Skeletal System: Axial Skeleton (Skull, Vertebrae, Ribs, Sternum - 80 bones)",
            "Human Skeletal System: Appendicular Skeleton (Pectoral/Pelvic Girdles, Limbs - 126 bones)",
            "Joints: Fibrous, Cartilaginous, Synovial Joints (Ball & Socket, Hinge, Pivot, Gliding, Saddle)",
            "Disorders: Myasthenia Gravis, Muscular Dystrophy, Tetany, Arthritis, Osteoporosis, Gout",
            "Past Years PYQ Practice"
        ),
        "Neural Control and Chemical Coordination" to listOf(
            "Human Nervous System Architecture: Central (CNS) & Peripheral (PNS: Somatic & Autonomic)",
            "Neuron Structure: Cell Body, Dendrites, Axon, Myelin Sheath & Nodes of Ranvier",
            "Generation & Conduction of Nerve Impulse: Resting Potential, Action Potential, Na+/K+ Pump",
            "Synaptic Transmission: Electrical vs Chemical Synapses, Neurotransmitter Release",
            "Human Brain Anatomy: Forebrain (Cerebrum, Thalamus, Hypothalamus, Limbic System)",
            "Human Brain Anatomy: Midbrain (Corpora Quadrigemina) & Hindbrain (Pons, Cerebellum, Medulla)",
            "Reflex Action & Reflex Arc Pathway",
            "Sensory Reception: Eye Anatomy (Retina, Rods, Cones, Rhodopsin) & Ear (Cochlea, Organ of Corti, Vestibular Apparatus)",
            "Endocrine Glands Overview: Hypothalamus Releasing/Inhibiting Hormones",
            "Pituitary Gland: Anterior (GH, PRL, TSH, ACTH, LH, FSH) & Posterior (Oxytocin, Vasopressin)",
            "Thyroid Gland (T3, T4, TCT), Parathyroid (PTH & Ca2+ Balance), Thymus (Thymosins)",
            "Adrenal Gland: Adrenal Cortex (Corticoids: Aldosterone, Cortisol) & Adrenal Medulla (Adrenaline)",
            "Pancreas (Islets: Glucagon, Insulin), Gonads (Testis: Testosterone, Ovary: Estrogen, Progesterone)",
            "Hormones of Heart (ANF), Kidney (Erythropoietin) & GI Tract (Gastrin, Secretin, CCK, GIP)",
            "Mechanism of Hormone Action: Peptide Hormones (cAMP, IP3) vs Steroid Hormones (Gene Expression)",
            "Past Years PYQ Practice"
        ),
        "Sexual Reproduction in Flowering Plants" to listOf(
            "Flower Structure & Pre-Fertilization Changes",
            "Stamen, Microsporangium & Microsporogenesis (Pollen Grain Structure, Sporopollenin)",
            "Pistil, Megasporangium (Ovule Types & Anatomy) & Megasporogenesis",
            "Development of Female Gametophyte: 7-Celled, 8-Nucleate Embryo Sac",
            "Pollination Types: Autogamy, Geitonogamy, Xenogamy & Agents of Pollination",
            "Outbreeding Devices to Promote Cross-Pollination",
            "Pollen-Pistil Interaction & Artificial Hybridization (Emasculation, Bagging)",
            "Double Fertilization: Syngamy & Triple Fusion (Primary Endosperm Nucleus PEN)",
            "Post-Fertilization: Endosperm Types (Nuclear, Cellular) & Embryo Development",
            "Seed & Fruit Formation, Pericarp & Seed Dormancy",
            "Apomixis, Parthenocarpy & Polyembryony Mechanisms",
            "Past Years PYQ Practice"
        ),
        "Human Reproduction & Reproductive Health" to listOf(
            "Male Reproductive System: Testes, Seminiferous Tubules, Sertoli Cells, Leydig Cells, Accessory Ducts/Glands",
            "Female Reproductive System: Ovaries, Fallopian Tubes, Uterus, Vagina, External Genitalia, Mammary Glands",
            "Spermatogenesis: Hormonal Regulation, Spermiogenesis, Spermiation & Sperm Anatomy",
            "Oogenesis: Folliculogenesis (Primordial to Graafian Follicle), Polar Body Formation",
            "Menstrual Cycle: Hormonal Control (FSH, LH Surge, Estrogen, Progesterone) & Phases",
            "Fertilization: Capacitation, Acrosome Reaction, Cortical Reaction, Implantation of Blastocyst",
            "Pregnancy & Placenta Hormones (hCG, hPL, Relaxin) & Embryonic Development",
            "Parturition: Foetal Ejection Reflex, Oxytocin & Lactation (Colostrum)",
            "Reproductive Health: Population Problems, Contraceptive Methods (Barrier, IUDs, Oral Pills: Saheli, Surgical)",
            "Medical Termination of Pregnancy (MTP Act) & Sexually Transmitted Infections (STIs)",
            "Infertility & Assisted Reproductive Technologies (ART): IVF-ET, ZIFT, GIFT, ICSI, IUI",
            "Past Years PYQ Practice"
        ),
        "Principles of Inheritance and Variation (Genetics I)" to listOf(
            "Mendel's Laws: Monohybrid Cross, Law of Segregation, Dominance & Recessiveness",
            "Dihybrid Cross, Law of Independent Assortment & Punnett Square Analysis",
            "Incomplete Dominance (Mirabilis jalapa) & Co-dominance (ABO Blood Groups)",
            "Multiple Allelism, Pleiotropy (Phenylketonuria) & Polygenic Inheritance",
            "Chromosomal Theory of Inheritance (Sutton & Boveri)",
            "Morgan's Drosophila Experiments: Linkage, Recombination & Genetic Mapping (Sturtevant)",
            "Sex Determination Mechanisms: XX-XY, XX-XO, ZZ-ZW & Honeybee Haplodiploidy",
            "Mutations: Point Mutation (Sickle Cell Anemia), Frameshift & Chromosomal Aberrations",
            "Pedigree Analysis: Symbols, Autosomal vs Sex-Linked Inheritance Patterns",
            "Mendelian Disorders: Hemophilia, Color Blindness, Thalassemia, Sickle Cell Anemia, Phenylketonuria",
            "Chromosomal Disorders: Aneuploidy, Down Syndrome (Trisomy 21), Turner (45, XO), Klinefelter (47, XXY)",
            "Past Years PYQ Practice"
        ),
        "Molecular Basis of Inheritance (Genetics II)" to listOf(
            "Search for Genetic Material: Griffith's Transformation Experiment",
            "Avery, MacLeod, McCarty Experiment & Hershey-Chase Bacteriophage Experiment",
            "Structure of DNA & RNA Polynucleotide Chains, Chargaff's Equivalence Rules",
            "Watson-Crick Double Helix Model & Packaging of DNA (Nucleosome, Histone Octamer)",
            "Meselson & Stahl Experiment on Semi-Conservative DNA Replication",
            "DNA Replication Mechanism: Replication Fork, DNA Polymerases, Okazaki Fragments",
            "Transcription Unit (Promoter, Structural Gene, Terminator) & Transcription in Prokaryotes",
            "Transcription in Eukaryotes: RNA Polymerases I, II, III & Post-Transcriptional Processing (Splicing, Capping, Tailing)",
            "Genetic Code: Properties (Triplet, Degenerate, Universal, Wobble Hypothesis)",
            "tRNA Adapter Molecule Structure & Translation Process (Charging, Initiation, Elongation, Termination)",
            "Regulation of Gene Expression: Lac Operon Model (Structural Genes z, y, a; Repressor, Inducer)",
            "Human Genome Project (HGP): Goals, Methodologies (ESTs, Sequence Annotation) & Salient Features",
            "DNA Fingerprinting: VNTRs, Southern Blotting Protocol & Applications",
            "Past Years PYQ Practice"
        ),
        "Evolution & Darwinism" to listOf(
            "Origin of Life Theories: Abiogenesis, Biogenesis, Oparin-Haldane & Urey-Miller Experiment",
            "Evidences for Evolution: Paleontological (Fossils) & Comparative Anatomy (Homology vs Analogy)",
            "Adaptive Radiation: Darwin's Finches & Australian Marsupials",
            "Embryological & Biochemical Evidences for Organic Evolution",
            "Evolution by Natural Selection: Industrial Melanism & Antibiotic/Pesticide Resistance",
            "Theories of Biological Evolution: Lamarckism, Darwin's Natural Selection & Hugo de Vries Mutation Theory (Saltation)",
            "Hardy-Weinberg Principle: Genetic Equilibrium Formula & 5 Disrupting Factors",
            "Types of Natural Selection: Stabilizing, Directional & Disruptive Selections",
            "Geological Time Scale Overview & Evolution of Plant and Animal Lineages",
            "Origin and Evolution of Man: Dryopithecus, Australopithecus, Homo habilis, Homo erectus, Neanderthal, Cro-Magnon, Homo sapiens",
            "Past Years PYQ Practice"
        ),
        "Human Health and Disease" to listOf(
            "Concept of Health, Disease & Common Bacterial Diseases: Typhoid (Widal Test), Pneumonia",
            "Common Viral Diseases: Common Cold (Rhino viruses), Dengue, Chikungunya",
            "Protozoan Diseases: Life Cycle of Plasmodium in Mosquito and Human Hosts, Amoebiasis",
            "Fungal Diseases (Ringworm) & Helminthic Diseases (Ascariasis, Filariasis/Elephantiasis)",
            "Innate Immunity: Physical, Physiological, Cellular & Cytokine Barriers",
            "Acquired Immunity: Active vs Passive Immunity, Primary & Secondary Immune Responses",
            "Structure of Antibody Molecule (H2L2), Ig Classes (IgA, IgG, IgM, IgE, IgD)",
            "Humoral vs Cell-Mediated Immunity (T-Cells, B-Cells, Organ Transplant Rejection)",
            "Vaccination, Immunisation & Autoimmunity / Autoimmune Diseases (Rheumatoid Arthritis)",
            "Immune System: Primary (Bone Marrow, Thymus) vs Secondary Lymphoid Organs (Spleen, Lymph Nodes, MALT)",
            "Allergies & Hypersensitivity (Histamine, IgE, Mast Cells, Anaphylaxis)",
            "AIDS: Causative Agent HIV Structure, Replication Cycle, Transmission, ELISA Test, Prevention",
            "Cancer: Characteristics (Loss of Contact Inhibition, Metastasis), Carcinogens, Diagnosis & Treatments",
            "Drug and Alcohol Abuse: Opioids, Cannabinoids, Coca Alkaloids, Hallucinogens, Tobacco & Alcohol Dependence",
            "Past Years PYQ Practice"
        ),
        "Microbes in Human Welfare" to listOf(
            "Microbes in Household Food Processing: Curd (LAB), Bread, Cheese (Swiss, Roquefort), Toddy",
            "Microbes in Industrial Production: Fermented Beverages & Antibiotics (Penicillin Discovery)",
            "Chemicals, Enzymes & Bioactive Molecules: Organic Acids, Streptokinase, Cyclosporin A, Statins",
            "Microbes in Sewage Treatment: Primary Treatment vs Secondary / Biological Treatment (Aerobic Flocs, BOD)",
            "Anaerobic Sludge Digesters & Biogas Generation Process",
            "Microbes in Biogas Production: Methanogens (Methanobacterium) & Biogas Plant Structure",
            "Microbes as Biocontrol Agents: Bacillus thuringiensis (Bt), Trichoderma, Baculoviruses (NPV)",
            "Microbes as Biofertilizers: Rhizobium, Azotobacter, Azospirillum, Mycorrhizae (Glomus), Cyanobacteria",
            "Past Years PYQ Practice"
        ),
        "Biotechnology: Principles and Processes" to listOf(
            "Principles of Biotechnology: Genetic Engineering & Bioprocess Engineering",
            "Tools of Recombinant DNA: Restriction Endonucleases (Recognition Sequence, Palindromes, Sticky Ends, EcoRI)",
            "Cloning Vectors: Origin of Replication (ori), Selectable Markers (ampR, tetR), Cloning Sites",
            "Insertional Inactivation & Blue-White Screening (beta-galactosidase gene)",
            "Vectors for Plants (Ti Plasmid of Agrobacterium tumefaciens) & Animals (Disarmed Retroviruses)",
            "Competent Host Preparation: Ca2+ Treatment, Heat Shock, Microinjection, Biolistics (Gene Gun)",
            "Isolation of Genetic Material: Lysis, Deproteinization & Chilled Ethanol Precipitation",
            "Cutting DNA & Agarose Gel Electrophoresis (EtBr Staining, UV Visualization, Elution)",
            "Polymerase Chain Reaction (PCR): Denaturation, Annealing, Extension with Taq Polymerase",
            "Ligation into Vector & Transformation of Recombinant DNA into Host",
            "Bioreactors: Simple Stirred-Tank & Sparged Stirred-Tank Bioreactors (Process Parameters)",
            "Downstream Processing: Separation, Purification, Formulation & Quality Control Testing",
            "Past Years PYQ Practice"
        ),
        "Biotechnology and its Applications" to listOf(
            "Biotechnological Applications in Agriculture: Advantages of Genetically Modified Organisms (GMOs)",
            "Bt Crops: Bt Cotton, Cry Proteins (CryIAc, CryIIAb, CryIAb) & Mechanism of Insecticidal Action",
            "Pest Resistant Plants: RNA Interference (RNAi / Gene Silencing using dsRNA against Nematodes)",
            "Applications in Medicine: Genetically Engineered Insulin (Humulin by Eli Lilly, Removal of C-Peptide)",
            "Gene Therapy: Treatment of ADA Deficiency in 4-Year-Old Girl (Retroviral Transfection)",
            "Molecular Diagnosis: Recombinant DNA Probes, PCR Diagnosis & ELISA Techniques",
            "Transgenic Animals: Objectives (Normal Physiology, Disease Models, Vaccine/Toxicity Safety)",
            "Biological Products: Transgenic Cow Rosie (Alpha-lactalbumin), Alpha-1 Antitrypsin for Emphysema",
            "Ethical Issues in Biotechnology: GEAC Roles, Biopiracy (Basmati Rice, Neem, Turmeric Patents)",
            "Past Years PYQ Practice"
        ),
        "Organisms, Populations & Ecosystem" to listOf(
            "Organism and Environment: Major Abiotic Factors (Temperature, Water, Light, Soil)",
            "Responses to Abiotic Factors: Regulators, Conformers, Migration, Suspension (Diapause)",
            "Adaptations: Morphological, Physiological & Behavioral (Kangaroo Rat, Desert Plants, Allen's Rule)",
            "Population Attributes: Birth/Death Rates, Sex Ratio, Age Pyramids (Expanding, Stable, Declining)",
            "Population Growth Models: Exponential Growth (dN/dt = rN) vs Logistic Growth (Verhulst-Pearl)",
            "Population Interactions: Mutualism, Commensalism, Parasitism, Predation, Competition (Gause's Exclusion), Amensalism",
            "Ecosystem Structure: Biotic & Abiotic Components, Stratification, Primary & Secondary Productivity (GPP, NPP)",
            "Decomposition: Steps (Fragmentation, Leaching, Catabolism, Humification, Mineralization)",
            "Energy Flow in Ecosystem: Grazing vs Detritus Food Chains, 10% Energy Transfer Law",
            "Ecological Pyramids: Pyramid of Numbers, Biomass, Energy & Inverted Pyramids",
            "Ecological Succession: Primary vs Secondary Succession, Hydrarch & Xerarch Seral Stages to Climax",
            "Nutrient Cycles: Carbon & Phosphorus Cycles, Ecosystem Services (Robert Costanza Assessment)",
            "Past Years PYQ Practice"
        ),
        "Biodiversity and Conservation" to listOf(
            "Concept & Levels of Biodiversity: Genetic, Species & Ecological Diversity",
            "Magnitude of Global & Indian Biodiversity (Robert May's 7 Million Species Estimate)",
            "Patterns of Biodiversity: Latitudinal Gradients & Species-Area Relationship (Alexander von Humboldt)",
            "Importance of Biodiversity: David Tilman's Experiments & Paul Ehrlich's Rivet Popper Hypothesis",
            "Loss of Biodiversity: 'The Evil Quartet' (Habitat Loss, Over-Exploitation, Alien Species, Co-Extinction)",
            "Reasons for Conservation: Narrowly Utilitarian, Broadly Utilitarian & Ethical Arguments",
            "Conservation Strategies: In-situ Conservation (National Parks, Wildlife Sanctuaries, Biosphere Reserves, Sacred Groves, Hotspots)",
            "Conservation Strategies: Ex-situ Conservation (Botanical Gardens, Zoological Parks, Cryopreservation, Seed Banks)",
            "International Conventions: Earth Summit (Rio de Janeiro 1992) & World Summit (Johannesburg 2002)",
            "Past Years PYQ Practice"
        )
    )

    private val defaultGenericSubtopics = listOf(
        "Theory & NCERT Line-by-Line Reading",
        "Key Formulae, Concepts & Derivations",
        "Solved Classroom Examples & Numerical Problems",
        "NCERT Exemplar & In-Text Exercises",
        "Topic-Wise MCQ Practice & DPPs",
        "NEET Past 10 Years PYQs Solving",
        "High-Yield Summary Notes & Flashcards Revision",
        "Chapter Test & Error Log Analysis"
    )

    fun getSubtopicsForChapter(chapterName: String): List<String> {
        val cleanName = chapterName.trim()
        
        // 1. Direct match
        neetChapterSubtopics[cleanName]?.let { return it }

        // 2. Case-insensitive exact match
        neetChapterSubtopics.entries.firstOrNull { it.key.equals(cleanName, ignoreCase = true) }?.let {
            return it.value
        }

        // 3. Keyword / partial matching
        val lower = cleanName.lowercase()
        val match = neetChapterSubtopics.entries.firstOrNull { (key, _) ->
            val keyLower = key.lowercase()
            val cleanKey = keyLower.replace(Regex("[^a-z0-9]"), " ")
            val cleanTarget = lower.replace(Regex("[^a-z0-9]"), " ")
            cleanTarget.contains(cleanKey) || cleanKey.contains(cleanTarget) ||
            (lower.contains("units") && keyLower.contains("units")) ||
            (lower.contains("straight line") && keyLower.contains("straight line")) ||
            (lower.contains("plane") && keyLower.contains("plane")) ||
            (lower.contains("laws of motion") && keyLower.contains("laws of motion")) ||
            (lower.contains("work") && keyLower.contains("work")) ||
            (lower.contains("rotation") && keyLower.contains("rotational")) ||
            (lower.contains("gravitation") && keyLower.contains("gravitation")) ||
            (lower.contains("solids") && keyLower.contains("solids")) ||
            (lower.contains("fluids") && keyLower.contains("fluids")) ||
            (lower.contains("thermal") && keyLower.contains("thermal")) ||
            (lower.contains("thermodynamics") && keyLower.contains("thermodynamics")) ||
            (lower.contains("kinetic theory") && keyLower.contains("kinetic theory")) ||
            (lower.contains("oscillat") && keyLower.contains("oscillat")) ||
            (lower.contains("waves") && keyLower.contains("waves")) ||
            (lower.contains("electrostat") && keyLower.contains("electrostat")) ||
            (lower.contains("current") && keyLower.contains("current")) ||
            (lower.contains("moving charges") && keyLower.contains("moving charges")) ||
            (lower.contains("magnetism") && keyLower.contains("magnetism")) ||
            (lower.contains("induction") && keyLower.contains("induction")) ||
            (lower.contains("alternating") && keyLower.contains("alternating")) ||
            (lower.contains("ray optics") && keyLower.contains("ray optics")) ||
            (lower.contains("wave optics") && keyLower.contains("wave optics")) ||
            (lower.contains("dual nature") && keyLower.contains("dual nature")) ||
            (lower.contains("atoms") && keyLower.contains("atoms")) ||
            (lower.contains("nuclei") && keyLower.contains("nuclei")) ||
            (lower.contains("semiconductor") && keyLower.contains("semiconductor")) ||
            (lower.contains("basic principles") && keyLower.contains("basic principles")) ||
            (lower.contains("goc") && keyLower.contains("goc")) ||
            (lower.contains("hydrocarbon") && keyLower.contains("hydrocarbon")) ||
            (lower.contains("haloalkane") && keyLower.contains("haloalkane")) ||
            (lower.contains("alcohol") && keyLower.contains("alcohol")) ||
            (lower.contains("aldehyde") && keyLower.contains("aldehyde")) ||
            (lower.contains("amine") && keyLower.contains("amine")) ||
            (lower.contains("biomolecule") && keyLower.contains("biomolecule")) ||
            (lower.contains("purification") && keyLower.contains("purification")) ||
            (lower.contains("periodic") && keyLower.contains("periodic")) ||
            (lower.contains("chemical bonding") && keyLower.contains("chemical bonding")) ||
            (lower.contains("d and f") && keyLower.contains("d and f")) ||
            (lower.contains("coordination") && keyLower.contains("coordination")) ||
            (lower.contains("p-block") && keyLower.contains("p-block")) ||
            (lower.contains("mole concept") && keyLower.contains("mole concept")) ||
            (lower.contains("basic concepts") && keyLower.contains("basic concepts")) ||
            (lower.contains("structure of atom") && keyLower.contains("structure of atom")) ||
            (lower.contains("equilibrium") && keyLower.contains("equilibrium")) ||
            (lower.contains("redox") && keyLower.contains("redox")) ||
            (lower.contains("solution") && keyLower.contains("solution")) ||
            (lower.contains("electrochem") && keyLower.contains("electrochem")) ||
            (lower.contains("kinetic") && keyLower.contains("kinetic")) ||
            (lower.contains("living world") && keyLower.contains("living world")) ||
            (lower.contains("biological classification") && keyLower.contains("biological classification")) ||
            (lower.contains("plant kingdom") && keyLower.contains("plant kingdom")) ||
            (lower.contains("animal kingdom") && keyLower.contains("animal kingdom")) ||
            (lower.contains("morphology") && keyLower.contains("morphology")) ||
            (lower.contains("anatomy") && keyLower.contains("anatomy")) ||
            (lower.contains("structural organisation") && keyLower.contains("structural organisation")) ||
            (lower.contains("cell") && keyLower.contains("cell")) ||
            (lower.contains("photosynthesis") && keyLower.contains("photosynthesis")) ||
            (lower.contains("respiration") && keyLower.contains("respiration")) ||
            (lower.contains("plant growth") && keyLower.contains("plant growth")) ||
            (lower.contains("breathing") && keyLower.contains("breathing")) ||
            (lower.contains("body fluids") && keyLower.contains("body fluids")) ||
            (lower.contains("excretory") && keyLower.contains("excretory")) ||
            (lower.contains("locomotion") && keyLower.contains("locomotion")) ||
            (lower.contains("neural") && keyLower.contains("neural")) ||
            (lower.contains("sexual reproduction") && keyLower.contains("sexual reproduction")) ||
            (lower.contains("human reproduction") && keyLower.contains("human reproduction")) ||
            (lower.contains("genetics") && keyLower.contains("genetics")) ||
            (lower.contains("inheritance") && keyLower.contains("inheritance")) ||
            (lower.contains("molecular basis") && keyLower.contains("molecular basis")) ||
            (lower.contains("evolution") && keyLower.contains("evolution")) ||
            (lower.contains("human health") && keyLower.contains("human health")) ||
            (lower.contains("microbes") && keyLower.contains("microbes")) ||
            (lower.contains("biotechnology") && keyLower.contains("biotechnology")) ||
            (lower.contains("organism") && keyLower.contains("organism")) ||
            (lower.contains("ecosystem") && keyLower.contains("ecosystem")) ||
            (lower.contains("biodiversity") && keyLower.contains("biodiversity"))
        }

        if (match != null) {
            return match.value
        }

        return defaultGenericSubtopics
    }

    fun getFullNeetChecklist(): List<DynamicChecklistTopic> {
        val list = mutableListOf<DynamicChecklistTopic>()
        val defaultCols = "2020,2021,2022,2023,2024,2025,2026"
        
        neetPhysicsChapters.forEach {
            list.add(DynamicChecklistTopic(subject = "Physics", phaseTitle = "Physics 100% Checklist", topicName = it.name, days = 3, columns = defaultCols))
        }
        neetChemistryChapters.forEach {
            list.add(DynamicChecklistTopic(subject = "Chemistry", phaseTitle = "Chemistry 100% Checklist", topicName = it.name, days = 3, columns = defaultCols))
        }
        neetBiologyChapters.forEach {
            list.add(DynamicChecklistTopic(subject = "Biology", phaseTitle = "Biology 100% Checklist", topicName = it.name, days = 2, columns = defaultCols))
        }
        return list
    }
}

