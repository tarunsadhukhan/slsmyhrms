package com.example.slsHrms.api

import com.example.slsHrms.permissions.MenuPermissionResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ── Dynamic Menu Permissions ──────────────────────────────────
    @GET(ApiRoutes.MENU_PERMISSIONS)
    fun getMenuPermissions(
        @Query("user_id") userId: Int
    ): Call<MenuPermissionResponse>


    @POST(ApiRoutes.LOGIN)
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @GET(ApiRoutes.GET_COMPANY)
    fun getCompanies(@Query("user_id") userId: Int): Call<CompanyResponse>

    @GET("companies")
    fun getCompaniesLegacy(): Call<CompanyResponse>

    @GET(ApiRoutes.GET_BRANCH)
    fun getBranches(
        @Query("company_id") companyId: Int,
        @Query("user_id") userId: Int
    ): Call<BranchResponse>

    @GET(ApiRoutes.GET_BRANCH)
    fun getBranchesByCoId(
        @Query("co_id") companyId: Int,
        @Query("user_id") userId: Int
    ): Call<BranchResponse>

    @GET("branches")
    fun getBranchesLegacy(@Query("company_id") companyId: Int): Call<BranchResponse>

    @GET(ApiRoutes.GET_DEPARTMENT)
    fun getDepartments(
        @Query("co_id") coId: Int? = null,
        @Query("branch_id") branchId: Int? = null
    ): Call<DepartmentResponse>

    @GET(ApiRoutes.DESIGNATIONS)
    fun getDesignations(
        @Query("branch_id") branchId: Int,
        @Query("sub_dept_id") subDeptId: Int? = null
    ): Call<DesignationResponse>

    @POST(ApiRoutes.DEPARTMENTS)
    fun addDepartment(@Body request: AddDepartmentRequest): Call<AddDepartmentResponse>

    @GET(ApiRoutes.SHIFTS)
    fun getShifts(
        @Query("branch_id") branchId: Int? = null
    ): Call<ShiftResponse>

    @POST(ApiRoutes.SHIFTS)
    fun addShift(@Body request: AddShiftRequest): Call<AddShiftResponse>

    @GET(ApiRoutes.OCCUPATIONS)
    fun getOccupations(): Call<OccupationResponse>

    @POST(ApiRoutes.OCCUPATIONS)
    fun addOccupation(@Body request: AddOccupationRequest): Call<AddOccupationResponse>

    @PUT("${ApiRoutes.OCCUPATIONS}/{id}")
    fun updateOccupation(@Path("id") id: Int, @Body request: AddOccupationRequest): Call<AddOccupationResponse>

    @DELETE("${ApiRoutes.OCCUPATIONS}/{id}")
    fun deleteOccupation(@Path("id") id: Int): Call<AddOccupationResponse>

    @POST(ApiRoutes.ATTENDANCE)
    fun recognizeFace(@Body request: FaceRecognitionRequest): Call<FaceRecognitionResponse>

    @POST(ApiRoutes.MARK_ATTENDANCE)
    fun markAttendanceManual(@Body request: MarkAttendanceRequest): Call<FaceRecognitionResponse>

    @POST(ApiRoutes.CHECK_FACE)
    fun checkFace(@Body request: FaceRecognitionRequest): Call<FaceRecognitionResponse>

    @GET(ApiRoutes.EMPLOYEE_BY_CODE)
    fun getEmployeeByCode(
        @Path("emp_code") empCode: String,
        @Query("branch_id") branchId: Int? = null
    ): Call<FaceRecognitionResponse>

    @GET(ApiRoutes.ATTENDANCE_LEAVE_STATUS)
    fun getAttendanceLeaveStatus(
        @Query("eb_id") ebId: Int,
        @Query("attendance_date") attendanceDate: String
    ): Call<LeaveStatusResponse>

    // ── Employees ────────────────────────────────────────────────
    @GET(ApiRoutes.EMPLOYEES)
    fun getEmployees(): Call<EmployeeResponse>

    @GET(ApiRoutes.EMPLOYEE_SEARCH)
    fun searchEmployees(
        @Query("q") query: String,
        @Query("branch_id") branchId: Int? = null
    ): Call<EmployeeResponse>

    @POST(ApiRoutes.REGISTER)
    fun addEmployee(@Body request: AddEmployeeRequest): Call<AddEmployeeResponse>

    @PUT("${ApiRoutes.EMPLOYEES}/{id}")
    fun updateEmployee(@Path("id") id: Int, @Body request: AddEmployeeRequest): Call<AddEmployeeResponse>

    @DELETE("${ApiRoutes.EMPLOYEES}/{id}")
    fun deleteEmployee(@Path("id") id: Int): Call<AddEmployeeResponse>

    // ── Dashboard Stats ────────────────────────────────────────
    @GET(ApiRoutes.DASHBOARD_STATS)
    fun getDashboardStats(
        @Query("date") date: String,
        @Query("branch_id") branchId: Int? = null,
        @Query("co_id") coId: Int? = null,
        @Query("spell_id") spellId: Int? = null
    ): Call<DashboardStatsResponse>

    @GET(ApiRoutes.DASHBOARD_CARD_TREND)
    fun getDashboardCardTrend(
        @Query("metric")    metric: String,
        @Query("branch_id") branchId: Int? = null,
        @Query("end_date")  endDate: String? = null
    ): Call<CardTrendResponse>

    // ── Emp-Wise Attendance Report ─────────────────────────────
    @GET(ApiRoutes.EMP_WISE_ATTENDANCE)
    fun getEmpWiseAttendance(
        @Query("from_date")      fromDate: String,
        @Query("to_date")        toDate: String,
        @Query("report_type")    reportType: String,
        @Query("branch_id")      branchId: Int? = null,
        @Query("spell_id")       spellId: Int? = null,
        @Query("dept_id")        deptId: Int? = null,
        @Query("designation_id") designationId: Int? = null,
        @Query("att_type")       attType: String? = null
    ): Call<EmpWiseAttendanceResponse>

    // ── Attendance Dashboard (charts) ──────────────────────────
    @GET(ApiRoutes.ATTENDANCE_DASHBOARD)
    fun getAttendanceDashboard(
        @Query("date") date: String,
        @Query("branch_id") branchId: Int? = null,
        @Query("co_id") coId: Int? = null,
        @Query("spell_id") spellId: Int? = null
    ): Call<AttendanceDashboardResponse>

    // ── Machines ───────────────────────────────────────────────
    @GET("machines")
    fun getMachines(
        @Query("designation_id") designationId: Int
    ): Call<MachineResponse>

    // ── Attendance Report ────────────────────────────────────────
    // Single date query (for Attendance Update page)
    @GET(ApiRoutes.ATTENDANCE_REPORT)
    fun getAttendanceReport(
        @Query("date") date: String,
        @Query("emp_code") empCode: String? = null,
        @Query("emp_name") empName: String? = null,
        @Query("shift_name") shiftName: String? = null,
        @Query("branch_id") branchId: Int? = null
    ): Call<AttendanceReportResponse>

    // Date range query (for Attendance Report page)
    @GET(ApiRoutes.ATTENDANCE_REPORT)
    fun getAttendanceReportRange(
        @Query("from_date") fromDate: String,
        @Query("to_date") toDate: String,
        @Query("department_id") departmentId: Int? = null,
        @Query("emp_code") empCode: String? = null,
        @Query("branch_id") branchId: Int? = null,
        @Query("shift_name") shiftName: String? = null,
        @Query("designation_id") designationId: Int? = null
    ): Call<AttendanceReportResponse>

    // ── Attendance Update ─────────────────────────────────────────
    @GET("attendance/{attendance_id}")
    fun getAttendanceById(@Path("attendance_id") attendanceId: Int): Call<AttendanceDetailResponse>

    @PUT("attendance/{attendance_id}")
    fun updateAttendance(
        @Path("attendance_id") attendanceId: Int,
        @Body request: UpdateAttendanceRequest
    ): Call<UpdateAttendanceResponse>

    // ── Leave Types ────────────────────────────────────────────
    @GET(ApiRoutes.LEAVE_TYPES)
    fun getLeaveTypes(): Call<LeaveTypeResponse>

    // ── Status Master ──────────────────────────────────────────
    @GET(ApiRoutes.STATUS_MST)
    fun getStatusMst(): Call<StatusMstResponse>

    // ── Leave Transactions ─────────────────────────────────────
    @GET(ApiRoutes.LEAVE_TRANSACTIONS)
    fun getLeaveTransactions(
        @Query("branch_id")   branchId: Int? = null,
        @Query("from_date")   fromDate: String? = null,
        @Query("to_date")     toDate: String? = null,
        @Query("emp_code")    empCode: String? = null,
        @Query("status_id")   statusId: Int? = null
    ): Call<LeaveListResponse>

    @POST(ApiRoutes.LEAVE_TRANSACTIONS)
    fun saveLeaveTransaction(@Body request: LeaveSaveRequest): Call<LeaveSaveResponse>

    @DELETE(ApiRoutes.LEAVE_TRANSACTION_DETAIL)
    fun deleteLeaveTransaction(@Path("id") id: Int): Call<LeaveSaveResponse>

    // ── Spinning Doff ─────────────────────────────────────────────
    @GET(ApiRoutes.SPELLS)
    fun getSpells(
        @Query("branch_id") branchId: Int? = null
    ): Call<SpellResponse>

    @GET(ApiRoutes.DOFF_MACHINES)
    fun getDoffMachines(@Query("branch_id") branchId: Int? = null): Call<DoffMachineResponse>

    @GET(ApiRoutes.DOFF_QUALITIES)
    fun getDoffQualities(@Query("branch_id") branchId: Int? = null): Call<SpinningQualityResponse>

    @GET(ApiRoutes.DOFF_TROLLIES)
    fun getDoffTrollies(@Query("branch_id") branchId: Int? = null): Call<TrollyResponse>

    @GET(ApiRoutes.DOFF_TRANSACTIONS)
    fun getDoffTransactions(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int? = null,
        @Query("branch_id") branchId: Int? = null,
        @Query("mc_id")     mcId: Int? = null
    ): Call<DoffListResponse>

    @GET(ApiRoutes.DOFF_LAST_BY_MACHINE)
    fun getDoffLastByMachine(@Query("mc_id") mcId: Int): Call<DoffDefaultsResponse>

    @GET(ApiRoutes.DOFF_LAST_EMP)
    fun getDoffLastEmp(
        @Query("date")     date: String,
        @Query("spell_id") spellId: Int
    ): Call<DoffDefaultsResponse>

    @POST(ApiRoutes.DOFF_TRANSACTIONS)
    fun saveDoffTransaction(@Body request: DoffSaveRequest): Call<DoffSaveResponse>

    @DELETE(ApiRoutes.DOFF_TRANSACTION_DETAIL)
    fun deleteDoffTransaction(@Path("id") id: Int): Call<DoffSaveResponse>

    @GET(ApiRoutes.DOFF_VALIDATE_MACHINE)
    fun validateDoffMachine(
        @Query("mc_no")     mcNo: String,
        @Query("branch_id") branchId: Int? = null
    ): Call<DoffMachineValidateResponse>

    @GET(ApiRoutes.DOFF_VALIDATE_TROLLY)
    fun validateDoffTrolly(
        @Query("trolly_no") trollyNo: String,
        @Query("branch_id") branchId: Int? = null
    ): Call<DoffTrollyValidateResponse>

    @GET(ApiRoutes.DOFF_SUMMARY)
    fun getDoffSummary(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int? = null,
        @Query("branch_id") branchId: Int? = null,
        @Query("mc_id")     mcId: Int? = null
    ): Call<DoffSummaryResponse>

    @GET(ApiRoutes.DOFF_FRAME_MACHINES)
    fun getFrameMachines(
        @Query("branch_id") branchId: Int
    ): Call<DoffMachineResponse>

    @GET(ApiRoutes.DOFF_FRAME_ENTRIES)
    fun getFrameEntries(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int
    ): Call<FrameEntriesResponse>

    @POST(ApiRoutes.DOFF_FRAME_ENTRIES)
    fun saveFrameEntries(@Body request: FrameEntriesSaveRequest): Call<FrameEntriesSaveResponse>

    @GET(ApiRoutes.DOFF_FRAME_MACHINE_DEFAULTS)
    fun getFrameMachineDefaults(
        @Query("branch_id") branchId: Int
    ): Call<FrameMachineDefaultsResponse>

    // ── Winding Frame Entry ────────────────────────────────────────
    @GET(ApiRoutes.DOFF_WINDING_FRAME_MACHINES)
    fun getWindingFrameMachines(
        @Query("branch_id") branchId: Int
    ): Call<DoffMachineResponse>

    @GET(ApiRoutes.DOFF_WINDING_FRAME_ENTRIES)
    fun getWindingFrameEntries(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int
    ): Call<FrameEntriesResponse>

    @POST(ApiRoutes.DOFF_WINDING_FRAME_ENTRIES)
    fun saveWindingFrameEntries(@Body request: FrameEntriesSaveRequest): Call<FrameEntriesSaveResponse>

    @GET(ApiRoutes.DOFF_WINDING_FRAME_DEFAULTS)
    fun getWindingFrameMachineDefaults(
        @Query("branch_id") branchId: Int
    ): Call<FrameMachineDefaultsResponse>

    @GET(ApiRoutes.DOFF_WINDING_QUALITIES)
    fun getWindingQualities(
        @Query("branch_id") branchId: Int? = null
    ): Call<WindingQualityResponse>

    @GET(ApiRoutes.DOFF_LAST_QUALITY_BY_MC)
    fun getLastQualityByMc(
        @Query("branch_id") branchId: Int,
        @Query("type") type: String = "S"
    ): Call<LastQualityByMcResponse>

    @GET(ApiRoutes.DOFF_WINDING_EB_LOOKUP)
    fun windingEbLookup(
        @Query("eb_no")     ebNo: Long,
        @Query("branch_id") branchId: Int
    ): Call<WindingEbLookupResponse>

    @GET(ApiRoutes.DOFF_WINDING_ENTRIES_LIST)
    fun getWindingEntries(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int
    ): Call<WindingEntriesResponse>

    @POST(ApiRoutes.DOFF_WINDING_ENTRY)
    fun saveWindingEntry(@Body request: WindingEntrySaveRequest): Call<WindingEntrySaveResponse>

    @PUT(ApiRoutes.DOFF_WINDING_ENTRY_DETAIL)
    fun updateWindingEntry(
        @Path("id") id: Int,
        @Body request: WindingEntryUpdateRequest
    ): Call<WindingEntrySaveResponse>

    @DELETE(ApiRoutes.DOFF_WINDING_ENTRY_DETAIL)
    fun deleteWindingEntry(@Path("id") id: Int): Call<WindingEntrySaveResponse>

    // ── Continuous Winding Entry (date + quality + prod_kgs) ───────────────────
    @GET(ApiRoutes.DOFF_CONT_WINDING_ENTRIES)
    fun getContWindingEntries(
        @Query("date") date: String
    ): Call<ContWindingEntriesResponse>

    @POST(ApiRoutes.DOFF_CONT_WINDING_ENTRY)
    fun saveContWindingEntry(@Body request: ContWindingEntrySaveRequest): Call<ContWindingEntrySaveResponse>

    @DELETE(ApiRoutes.DOFF_CONT_WINDING_ENTRY_DETAIL)
    fun deleteContWindingEntry(@Path("id") id: Int): Call<ContWindingEntrySaveResponse>

    // ── On Boarding ───────────────────────────────────────────────
    @GET(ApiRoutes.ONBOARDING_EMPLOYEE)
    fun getOnBoardingEmployee(
        @Path("emp_code") empCode: String,
        @Query("branch_id") branchId: Int? = null
    ): Call<OnBoardingEmployeeResponse>

    @POST(ApiRoutes.ONBOARDING_REGISTER_FACE)
    fun registerOnBoardingFace(@Body request: OnBoardingRegisterRequest): Call<OnBoardingRegisterResponse>

    // ── Weight Entry ──────────────────────────────────────────────────
    @GET(ApiRoutes.WEIGHT_TRANSACTIONS)
    fun getWeightTransactions(
        @Query("branch_id") branchId: Int? = null,
        @Query("date")      date: String? = null
    ): Call<WeightListResponse>

    @POST(ApiRoutes.WEIGHT_TRANSACTIONS)
    fun saveWeightTransaction(@Body request: WeightSaveRequest): Call<WeightSaveResponse>

    @DELETE(ApiRoutes.WEIGHT_TRANSACTION_DETAIL)
    fun deleteWeightTransaction(@Path("id") id: Int): Call<WeightSaveResponse>

    // ── Spg Doff Entry (1) ────────────────────────────────────────────────────
    @GET(ApiRoutes.DOFF_SPG1_MECH_CODES)
    fun getSpg1MechCodes(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int
    ): Call<Spg1MechCodesResponse>

    @GET(ApiRoutes.DOFF_SPG1_SUMMARY)
    fun getSpg1Summary(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int
    ): Call<Spg1SummaryResponse>

    @GET(ApiRoutes.DOFF_SPG1_QUALITY_SHIFT_REPORT)
    fun getSpg1QualityShiftReport(
        @Query("date")      date: String,
        @Query("branch_id") branchId: Int
    ): Call<QualityShiftReportResponse>

    // ── Winding Entry (2) ──────────────────────────────────────────────────
    @GET(ApiRoutes.DOFF_WE2_EMP_LOOKUP)
    fun we2EmpLookup(
        @Query("emp_code")  empCode: String,
        @Query("branch_id") branchId: Int
    ): Call<We2EmpLookupResponse>

    @GET(ApiRoutes.DOFF_WE2_EMPLOYEES)
    fun getWe2Employees(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int
    ): Call<We2EmployeesResponse>

    @GET(ApiRoutes.DOFF_WE2)
    fun getWe2Summary(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int
    ): Call<We2SummaryResponse>

    @GET(ApiRoutes.DOFF_WE2_DETAIL_BY_EMP)
    fun getWe2Details(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int,
        @Query("eb_id")     ebId: Long
    ): Call<We2SummaryResponse>

    @GET(ApiRoutes.DOFF_WE2_SUMMARY)
    fun getWe2GroupedSummary(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int
    ): Call<We2GroupedResponse>

    @POST(ApiRoutes.DOFF_WE2)
    fun saveWe2(@Body req: We2SaveRequest): Call<We2SaveResponse>

    @DELETE(ApiRoutes.DOFF_WE2_DETAIL)
    fun deleteWe2(@Path("id") id: Int): Call<We2SaveResponse>

    @GET(ApiRoutes.DOFF_WE2_QUALITY_SHIFT_REPORT)
    fun getWe2QualityShiftReport(
        @Query("date")      date: String,
        @Query("branch_id") branchId: Int
    ): Call<QualityShiftReportResponse>

    // ── Drawing Meter Entry ────────────────────────────────────────
    @GET(ApiRoutes.DRAWING_SHEDS)
    fun getDrawingSheds(
        @Query("branch_id") branchId: Int? = null
    ): Call<DrawingShedsResponse>

    @GET(ApiRoutes.DRAWING_MACHINES)
    fun getDrawingMachines(
        @Query("shed_type") shedType: String,
        @Query("branch_id") branchId: Int? = null
    ): Call<DrawingMachinesResponse>

    @GET(ApiRoutes.DRAWING_OPENING_METER)
    fun getDrawingOpeningMeter(
        @Query("date")     date: String,
        @Query("spell_id") spellId: Int,
        @Query("mc_id")    mcId: Int
    ): Call<DrawingOpeningMeterResponse>

    @POST(ApiRoutes.DRAWING_ENTRY)
    fun saveDrawingEntry(@Body request: DrawingEntrySaveRequest): Call<DrawingEntrySaveResponse>

    @GET(ApiRoutes.DRAWING_SUMMARY)
    fun getDrawingSummary(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int? = null
    ): Call<DrawingSummaryResponse>

    @GET(ApiRoutes.DRAWING_SPELLS)
    fun getDrawingSpells(
        @Query("branch_id") branchId: Int? = null
    ): Call<SpellResponse>

    // ── Spreader Production Entry ─────────────────────────────────────────────
    @GET(ApiRoutes.SPREADER_MACHINES)
    fun getSpreaderMachines(
        @Query("branch_id") branchId: Int
    ): Call<SpreaderMachineResponse>

    @GET(ApiRoutes.SPREADER_QUALITIES)
    fun getSpreaderQualities(
        @Query("branch_id") branchId: Int
    ): Call<SpreaderQualityResponse>

    @GET(ApiRoutes.SPREADER_SPRD_QUALITIES)
    fun getSpreaderSprdQualities(
        @Query("branch_id") branchId: Int
    ): Call<SpreaderQualityResponse>

    @GET(ApiRoutes.SPREADER_PROD_ENTRY)
    fun getSpreaderProdEntries(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int
    ): Call<SpreaderProdEntryListResponse>

    @POST(ApiRoutes.SPREADER_PROD_ENTRY)
    fun saveSpreaderProdEntry(@Body req: SpreaderProdSaveRequest): Call<SpreaderProdSaveResponse>

    @PUT(ApiRoutes.SPREADER_PROD_ENTRY_ID)
    fun updateSpreaderProdEntry(
        @Path("id") id: Int,
        @Body req: SpreaderProdUpdateRequest
    ): Call<SpreaderProdSaveResponse>

    @DELETE(ApiRoutes.SPREADER_PROD_ENTRY_ID)
    fun deleteSpreaderProdEntry(@Path("id") id: Int): Call<SpreaderProdSaveResponse>

    @GET(ApiRoutes.SPREADER_ISSUE_ENTRY)
    fun getSpreaderIssueEntries(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int
    ): Call<SpreaderIssueEntryListResponse>

    @POST(ApiRoutes.SPREADER_ISSUE_ENTRY)
    fun saveSpreaderIssueEntry(@Body req: SpreaderIssueSaveRequest): Call<SpreaderIssueSaveResponse>

    @PUT(ApiRoutes.SPREADER_ISSUE_ENTRY_ID)
    fun updateSpreaderIssueEntry(
        @Path("id") id: Int,
        @Body req: SpreaderIssueUpdateRequest
    ): Call<SpreaderIssueSaveResponse>

    @DELETE(ApiRoutes.SPREADER_ISSUE_ENTRY_ID)
    fun deleteSpreaderIssueEntry(@Path("id") id: Int): Call<SpreaderIssueSaveResponse>

    @GET(ApiRoutes.SPREADER_QUALITY_STOCK)
    fun getSpreaderQualityStock(
        @Query("quality_id") qualityId: Int
    ): Call<SpreaderQualityStockResponse>

    @GET(ApiRoutes.SPREADER_QUALITY_STOCK_LIST)
    fun getSpreaderQualityStockList(
        @Query("branch_id") branchId: Int
    ): Call<SpreaderQualityStockListResponse>

    // ── Assorting Entry ───────────────────────────────────────────────────────
    @GET(ApiRoutes.ASSORTING_SHEDS)
    fun getAssortingSheds(
        @Query("branch_id") branchId: Int
    ): Call<AssortingShedsResponse>

    @GET(ApiRoutes.ASSORTING_MACHINES)
    fun getAssortingMachines(
        @Query("branch_id") branchId: Int,
        @Query("shed_type") shedType: String? = null
    ): Call<SpreaderMachineResponse>

    @GET(ApiRoutes.ASSORTING_QUALITIES)
    fun getAssortingQualities(
        @Query("branch_id") branchId: Int
    ): Call<SpreaderQualityResponse>

    @GET(ApiRoutes.ASSORTING_SELECTORS)
    fun getAssortingSelectors(
        @Query("branch_id") branchId: Int
    ): Call<SelectorResponse>

    @GET(ApiRoutes.ASSORTING_VALIDATE_TROLLY)
    fun validateAssortingTrolly(
        @Query("trolly_no") trollyNo: String,
        @Query("branch_id") branchId: Int? = null
    ): Call<AssortingTrollyValidateResponse>

    @POST(ApiRoutes.ASSORTING_ENTRY)
    fun saveAssortingEntry(@Body req: AssortingSaveRequest): Call<AssortingSaveResponse>

    @GET(ApiRoutes.ASSORTING_ENTRY)
    fun getAssortingEntries(
        @Query("date")      date: String,
        @Query("branch_id") branchId: Int,
        @Query("mc_id")     mcId: Int? = null
    ): Call<AssortingEntryListResponse>

    @PUT(ApiRoutes.ASSORTING_ENTRY_ID)
    fun updateAssortingEntry(
        @Path("id") id: Int,
        @Body req: AssortingSaveRequest
    ): Call<AssortingSaveResponse>

    @DELETE(ApiRoutes.ASSORTING_ENTRY_ID)
    fun deleteAssortingEntry(@Path("id") id: Int): Call<AssortingSaveResponse>

    // ── Jute Received ─────────────────────────────────────────────────────────
    @GET(ApiRoutes.JUTE_RECEIVED_QUALITIES)
    fun getJuteReceivedQualities(
        @Query("branch_id") branchId: Int
    ): Call<SpreaderQualityResponse>

    @POST(ApiRoutes.JUTE_RECEIVED_ENTRY)
    fun saveJuteReceivedEntry(@Body req: JuteReceivedSaveRequest): Call<JuteReceivedSaveResponse>

    @PUT(ApiRoutes.JUTE_RECEIVED_ENTRY_ID)
    fun updateJuteReceivedEntry(
        @Path("id") id: Int,
        @Body req: JuteReceivedSaveRequest
    ): Call<JuteReceivedSaveResponse>

    @GET(ApiRoutes.JUTE_RECEIVED_ENTRY)
    fun getJuteReceivedEntries(
        @Query("date")      date: String,
        @Query("branch_id") branchId: Int
    ): Call<JuteReceivedEntryListResponse>

    @DELETE(ApiRoutes.JUTE_RECEIVED_ENTRY_ID)
    fun deleteJuteReceivedEntry(@Path("id") id: Int): Call<JuteReceivedSaveResponse>

    // ── Finishing → Other Entries ─────────────────────────────────────────────
    @GET(ApiRoutes.FINISHING_ENTRY)
    fun getFinishingEntries(
        @Query("date")     date: String,
        @Query("spell_id") spellId: Int? = null
    ): Call<FinishingEntryListResponse>

    @POST(ApiRoutes.FINISHING_ENTRY)
    fun saveFinishingEntry(@Body req: FinishingSaveRequest): Call<FinishingSaveResponse>

    @PUT(ApiRoutes.FINISHING_ENTRY_ID)
    fun updateFinishingEntry(
        @Path("id") id: Int,
        @Body req: FinishingUpdateRequest
    ): Call<FinishingSaveResponse>

    @DELETE(ApiRoutes.FINISHING_ENTRY_ID)
    fun deleteFinishingEntry(@Path("id") id: Int): Call<FinishingSaveResponse>

    // ── Bales Production → Roll Stock ─────────────────────────────────────────
    @GET(ApiRoutes.BALESPROD_MC_CODES)
    fun getRollStockMcCodes(
        @Query("branch_id") branchId: Int
    ): Call<McCodeResponse>

    @GET(ApiRoutes.BALESPROD_ROLL_STOCK)
    fun getRollStockEntries(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int? = null
    ): Call<RollStockListResponse>

    @POST(ApiRoutes.BALESPROD_ROLL_STOCK)
    fun saveRollStock(@Body req: RollStockSaveRequest): Call<RollStockSaveResponse>

    @PUT(ApiRoutes.BALESPROD_ROLL_STOCK_ID)
    fun updateRollStock(
        @Path("id") id: Int,
        @Body req: RollStockUpdateRequest
    ): Call<RollStockSaveResponse>

    @DELETE(ApiRoutes.BALESPROD_ROLL_STOCK_ID)
    fun deleteRollStock(@Path("id") id: Int): Call<RollStockSaveResponse>

    // ── Bales Production / Issue Entry ────────────────────────────────────────
    @GET(ApiRoutes.BALESPROD_FNG_QUALITIES)
    fun getFinishingQualities(): Call<FinishingQualityResponse>

    @GET(ApiRoutes.BALESPROD_CUSTOMERS)
    fun getCustomers(): Call<CustomerResponse>

    @GET(ApiRoutes.BALESPROD_PROD_ENTRY)
    fun getBalesProdEntries(
        @Query("date")     date: String,
        @Query("spell_id") spellId: Int
    ): Call<BalesProdEntryListResponse>

    @POST(ApiRoutes.BALESPROD_PROD_ENTRY)
    fun saveBalesProdEntry(@Body req: BalesProdSaveRequest): Call<BalesProdSaveResponse>

    @PUT(ApiRoutes.BALESPROD_PROD_ENTRY_ID)
    fun updateBalesProdEntry(
        @Path("id") id: Int,
        @Body req: BalesProdUpdateRequest
    ): Call<BalesProdSaveResponse>

    @DELETE(ApiRoutes.BALESPROD_PROD_ENTRY_ID)
    fun deleteBalesProdEntry(@Path("id") id: Int): Call<BalesProdSaveResponse>

    @GET(ApiRoutes.BALESPROD_ISSUE_ENTRY)
    fun getBalesIssueEntries(
        @Query("date")     date: String,
        @Query("spell_id") spellId: Int
    ): Call<BalesProdEntryListResponse>

    @POST(ApiRoutes.BALESPROD_ISSUE_ENTRY)
    fun saveBalesIssueEntry(@Body req: BalesIssueSaveRequest): Call<BalesProdSaveResponse>

    @PUT(ApiRoutes.BALESPROD_ISSUE_ENTRY_ID)
    fun updateBalesIssueEntry(
        @Path("id") id: Int,
        @Body req: BalesIssueUpdateRequest
    ): Call<BalesProdSaveResponse>

    @DELETE(ApiRoutes.BALESPROD_ISSUE_ENTRY_ID)
    fun deleteBalesIssueEntry(@Path("id") id: Int): Call<BalesProdSaveResponse>

    @GET(ApiRoutes.BALESPROD_STOCK)
    fun getBalesStock(): Call<BalesStockResponse>

    @GET(ApiRoutes.BALESPROD_STOCK_PAIR)
    fun getBalesStockPair(
        @Query("fng_quality_id") fngQualityId: Int,
        @Query("customer_id")    customerId: Int
    ): Call<BalesStockPairResponse>

    // ── Spg Running Hours ────────────────────────────────────────────────────
    @GET(ApiRoutes.SPG_RUNNING_HOURS)
    fun getSpgRunningHours(
        @Query("date")      date: String,
        @Query("spell_id")  spellId: Int,
        @Query("branch_id") branchId: Int? = null
    ): Call<SpgRunningListResponse>

    @POST(ApiRoutes.SPG_RUNNING_HOURS)
    fun saveSpgRunningHours(@Body req: SpgRunningSaveRequest): Call<SpgRunningSaveResponse>

    @PUT(ApiRoutes.SPG_RUNNING_HOURS_ID)
    fun updateSpgRunningHours(
        @Path("id") id: Int,
        @Body req: SpgRunningUpdateRequest
    ): Call<SpgRunningSaveResponse>

    @DELETE(ApiRoutes.SPG_RUNNING_HOURS_ID)
    fun deleteSpgRunningHours(@Path("id") id: Int): Call<SpgRunningSaveResponse>
}
