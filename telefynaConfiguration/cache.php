<?php
    $metrics = "Disk Usage: ".ceil((disk_free_space("/")/disk_total_space("/")) * 100)."%\r\n";
    file_put_contents("exports/".$now.".txt", $metrics.json_encode($_POST, JSON_PRETTY_PRINT)."\r\n______________________________________________________\r\n");
?>